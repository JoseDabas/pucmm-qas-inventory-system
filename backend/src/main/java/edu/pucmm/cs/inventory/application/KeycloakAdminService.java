package edu.pucmm.cs.inventory.application;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.keycloak.admin.client.CreatedResponseUtil;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import edu.pucmm.cs.inventory.infrastructure.security.Permissions;
import edu.pucmm.cs.inventory.infrastructure.security.SystemRole;
import edu.pucmm.cs.inventory.infrastructure.web.dto.CreateUserRequestDTO;
import edu.pucmm.cs.inventory.infrastructure.web.dto.UserResponseDTO;
import edu.pucmm.cs.inventory.infrastructure.web.exception.UsernameAlreadyExistsException;
import jakarta.persistence.EntityNotFoundException;
import jakarta.ws.rs.core.Response;

/**
 * Servicio de Aplicación para la administración de cuentas contra Keycloak.
 * <p>
 * Traduce los "roles del sistema" ({@link SystemRole}) a permisos granulares
 * (realm roles) y los asigna a las cuentas mediante la Keycloak Admin API. El
 * backend nunca autoriza por nombre de rol: aquí solo se usan los permisos que
 * componen cada rol.
 */
@Service
public class KeycloakAdminService {

    private final Keycloak keycloak;
    private final String realm;

    public KeycloakAdminService(Keycloak keycloak, @Value("${keycloak.admin.realm}") String realm) {
        this.keycloak = keycloak;
        this.realm = realm;
    }

    /**
     * Crea una cuenta en Keycloak, le fija una contraseña inicial y le asigna los
     * permisos que componen el rol indicado.
     *
     * @return la cuenta creada con su rol y permisos efectivos
     */
    public UserResponseDTO createUser(CreateUserRequestDTO request) {
        UsersResource usersResource = realmResource().users();

        UserRepresentation user = new UserRepresentation();
        user.setUsername(request.getUsername());
        user.setEmail(request.getEmail());
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEnabled(true);
        user.setEmailVerified(true);

        String userId;
        try (Response response = usersResource.create(user)) {
            if (response.getStatus() == Response.Status.CONFLICT.getStatusCode()) {
                throw new UsernameAlreadyExistsException(
                        "Ya existe una cuenta con ese nombre de usuario o correo.");
            }
            if (response.getStatus() != Response.Status.CREATED.getStatusCode()) {
                throw new IllegalStateException(
                        "Keycloak rechazó la creación de la cuenta (HTTP " + response.getStatus() + ").");
            }
            userId = CreatedResponseUtil.getCreatedId(response);
        }

        UserResource userResource = usersResource.get(userId);
        userResource.resetPassword(passwordCredential(request.getPassword()));
        replacePermissions(userResource, request.getRole().getPermissions());

        return toResponseDTO(userResource.toRepresentation(), request.getRole().getPermissions());
    }

    /**
     * Lista todas las cuentas del realm con sus permisos efectivos y el rol del
     * sistema resuelto (si coincide con el catálogo).
     */
    public List<UserResponseDTO> listUsers() {
        UsersResource usersResource = realmResource().users();
        return usersResource.list().stream()
                .map(user -> toResponseDTO(user, assignedPermissions(usersResource.get(user.getId()))))
                .collect(Collectors.toList());
    }

    /**
     * Reemplaza el conjunto de permisos de una cuenta por el del rol indicado.
     */
    public UserResponseDTO changeUserRole(String userId, SystemRole role) {
        UserResource userResource = userResourceOrThrow(userId);
        replacePermissions(userResource, role.getPermissions());
        return toResponseDTO(userResource.toRepresentation(), role.getPermissions());
    }

    // ---------------------------------------------------------------------
    // Helpers privados
    // ---------------------------------------------------------------------

    private RealmResource realmResource() {
        return keycloak.realm(realm);
    }

    private UserResource userResourceOrThrow(String userId) {
        try {
            UserResource userResource = realmResource().users().get(userId);
            // Fuerza la resolución: si el usuario no existe, lanza NotFound.
            userResource.toRepresentation();
            return userResource;
        } catch (jakarta.ws.rs.NotFoundException ex) {
            throw new EntityNotFoundException("No existe una cuenta con el ID proporcionado: " + userId);
        }
    }

    private CredentialRepresentation passwordCredential(String password) {
        CredentialRepresentation credential = new CredentialRepresentation();
        credential.setType(CredentialRepresentation.PASSWORD);
        credential.setValue(password);
        credential.setTemporary(false);
        return credential;
    }

    /**
     * Deja al usuario exactamente con los permisos indicados: quita los permisos
     * gestionados que tenga de más y añade los que le falten. No toca roles
     * internos de Keycloak (default-roles, etc.).
     */
    private void replacePermissions(UserResource userResource, Set<String> desiredPermissions) {
        Set<String> current = assignedPermissions(userResource);

        List<RoleRepresentation> toRemove = current.stream()
                .filter(permission -> !desiredPermissions.contains(permission))
                .map(this::realmRole)
                .collect(Collectors.toList());

        List<RoleRepresentation> toAdd = desiredPermissions.stream()
                .filter(permission -> !current.contains(permission))
                .map(this::realmRole)
                .collect(Collectors.toList());

        if (!toRemove.isEmpty()) {
            userResource.roles().realmLevel().remove(toRemove);
        }
        if (!toAdd.isEmpty()) {
            userResource.roles().realmLevel().add(toAdd);
        }
    }

    /** Permisos de la aplicación actualmente asignados al usuario. */
    private Set<String> assignedPermissions(UserResource userResource) {
        return userResource.roles().realmLevel().listAll().stream()
                .map(RoleRepresentation::getName)
                .filter(Permissions.ALL::contains)
                .collect(Collectors.toSet());
    }

    private RoleRepresentation realmRole(String name) {
        return realmResource().roles().get(name).toRepresentation();
    }

    private UserResponseDTO toResponseDTO(UserRepresentation user, Set<String> permissions) {
        UserResponseDTO dto = new UserResponseDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setEnabled(Boolean.TRUE.equals(user.isEnabled()));
        dto.setPermissions(permissions);
        SystemRole.fromPermissions(permissions).ifPresent(role -> dto.setRole(role.name()));
        return dto;
    }
}
