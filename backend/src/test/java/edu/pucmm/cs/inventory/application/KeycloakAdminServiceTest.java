package edu.pucmm.cs.inventory.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.RoleMappingResource;
import org.keycloak.admin.client.resource.RoleResource;
import org.keycloak.admin.client.resource.RoleScopeResource;
import org.keycloak.admin.client.resource.RolesResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.representations.idm.RoleRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import edu.pucmm.cs.inventory.infrastructure.security.Permissions;
import edu.pucmm.cs.inventory.infrastructure.security.SystemRole;
import edu.pucmm.cs.inventory.infrastructure.web.dto.CreateUserRequestDTO;
import edu.pucmm.cs.inventory.infrastructure.web.dto.UserResponseDTO;
import edu.pucmm.cs.inventory.infrastructure.web.exception.UsernameAlreadyExistsException;

import jakarta.persistence.EntityNotFoundException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;

/**
 * Pruebas unitarias de {@link KeycloakAdminService}. Mockean la API fluida del
 * cliente admin de Keycloak para verificar la creación de cuentas (incluyendo
 * los conflictos y errores), el listado, el cambio de rol y la traducción de
 * roles del sistema a permisos granulares.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class KeycloakAdminServiceTest {

    private static final String REALM = "Inventario";

    @Mock private Keycloak keycloak;
    @Mock private RealmResource realmResource;
    @Mock private UsersResource usersResource;
    @Mock private UserResource userResource;
    @Mock private RolesResource rolesResource;
    @Mock private RoleResource roleResource;
    @Mock private RoleMappingResource roleMappingResource;
    @Mock private RoleScopeResource roleScopeResource;

    private KeycloakAdminService service;

    @BeforeEach
    void setUp() {
        service = new KeycloakAdminService(keycloak, REALM);

        when(keycloak.realm(REALM)).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);

        // Resolución de realm roles por nombre → un RoleRepresentation cualquiera
        // (su contenido no afecta las verificaciones de add/remove).
        when(realmResource.roles()).thenReturn(rolesResource);
        when(rolesResource.get(anyString())).thenReturn(roleResource);
        when(roleResource.toRepresentation()).thenReturn(role("managed-role"));

        // Mapeo de roles a nivel de realm del usuario.
        when(userResource.roles()).thenReturn(roleMappingResource);
        when(roleMappingResource.realmLevel()).thenReturn(roleScopeResource);
    }

    private RoleRepresentation role(String name) {
        RoleRepresentation rep = new RoleRepresentation();
        rep.setName(name);
        return rep;
    }

    private UserRepresentation userRep(String id, String username) {
        UserRepresentation rep = new UserRepresentation();
        rep.setId(id);
        rep.setUsername(username);
        rep.setEnabled(true);
        return rep;
    }

    private CreateUserRequestDTO createRequest(SystemRole role) {
        CreateUserRequestDTO req = new CreateUserRequestDTO();
        req.setUsername("jperez");
        req.setEmail("jperez@inventario.local");
        req.setFirstName("Juan");
        req.setLastName("Pérez");
        req.setPassword("Cambiar123!");
        req.setRole(role);
        return req;
    }

    @Test
    @DisplayName("createUser crea la cuenta, fija la contraseña y asigna los permisos del rol")
    void createUserAsignaPermisos() {
        Response created = Response.created(URI.create("http://kc/admin/realms/Inventario/users/u-123")).build();
        when(usersResource.create(any(UserRepresentation.class))).thenReturn(created);
        when(usersResource.get("u-123")).thenReturn(userResource);
        when(roleScopeResource.listAll()).thenReturn(List.of()); // cuenta nueva sin permisos gestionados
        when(userResource.toRepresentation()).thenReturn(userRep("u-123", "jperez"));

        UserResponseDTO result = service.createUser(createRequest(SystemRole.VIEWER));

        assertThat(result.getId()).isEqualTo("u-123");
        assertThat(result.getUsername()).isEqualTo("jperez");
        assertThat(result.getRole()).isEqualTo("VIEWER");
        assertThat(result.getPermissions()).containsExactlyInAnyOrderElementsOf(SystemRole.VIEWER.getPermissions());
        verify(userResource).resetPassword(any());
        verify(roleScopeResource).add(anyList()); // se añaden los permisos del rol
    }

    @Test
    @DisplayName("createUser lanza UsernameAlreadyExistsException cuando Keycloak responde 409")
    void createUserConflicto() {
        when(usersResource.create(any(UserRepresentation.class)))
                .thenReturn(Response.status(Response.Status.CONFLICT).build());

        assertThatThrownBy(() -> service.createUser(createRequest(SystemRole.VIEWER)))
                .isInstanceOf(UsernameAlreadyExistsException.class);
    }

    @Test
    @DisplayName("createUser lanza IllegalStateException ante una respuesta inesperada de Keycloak")
    void createUserErrorInesperado() {
        when(usersResource.create(any(UserRepresentation.class)))
                .thenReturn(Response.status(Response.Status.INTERNAL_SERVER_ERROR).build());

        assertThatThrownBy(() -> service.createUser(createRequest(SystemRole.VIEWER)))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("listUsers devuelve las cuentas con su rol resuelto desde los permisos")
    void listUsersResuelveRol() {
        when(usersResource.list()).thenReturn(List.of(userRep("u-1", "viewer")));
        when(usersResource.get("u-1")).thenReturn(userResource);
        when(roleScopeResource.listAll()).thenReturn(List.of(
                role(Permissions.PRODUCT_VIEW),
                role(Permissions.STOCK_VIEW),
                role(Permissions.REPORT_VIEW),
                role("default-roles-inventario"))); // se ignora por no ser permiso gestionado

        List<UserResponseDTO> users = service.listUsers();

        assertThat(users).hasSize(1);
        assertThat(users.get(0).getRole()).isEqualTo("VIEWER");
        assertThat(users.get(0).getPermissions()).containsExactlyInAnyOrderElementsOf(SystemRole.VIEWER.getPermissions());
    }

    @Test
    @DisplayName("changeUserRole reemplaza los permisos: quita los sobrantes y añade los faltantes")
    void changeUserRoleReemplazaPermisos() {
        when(usersResource.get("u-1")).thenReturn(userResource);
        when(userResource.toRepresentation()).thenReturn(userRep("u-1", "jperez"));
        // Permisos actuales: product:view + audit:view (+ rol interno ignorado).
        when(roleScopeResource.listAll()).thenReturn(List.of(
                role(Permissions.PRODUCT_VIEW),
                role(Permissions.AUDIT_VIEW),
                role("default-roles-inventario")));

        UserResponseDTO result = service.changeUserRole("u-1", SystemRole.VIEWER);

        assertThat(result.getRole()).isEqualTo("VIEWER");
        // audit:view sobra → se quita; stock:view y report:view faltan → se añaden.
        verify(roleScopeResource).remove(anyList());
        verify(roleScopeResource).add(anyList());
    }

    @Test
    @DisplayName("changeUserRole lanza EntityNotFoundException si la cuenta no existe")
    void changeUserRoleNoExiste() {
        when(usersResource.get("inexistente")).thenReturn(userResource);
        when(userResource.toRepresentation()).thenThrow(new NotFoundException());

        assertThatThrownBy(() -> service.changeUserRole("inexistente", SystemRole.VIEWER))
                .isInstanceOf(EntityNotFoundException.class);
        verify(roleScopeResource, never()).add(anyList());
    }
}
