package edu.pucmm.cs.inventory.infrastructure.web;

import java.util.Arrays;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.pucmm.cs.inventory.application.KeycloakAdminService;
import edu.pucmm.cs.inventory.infrastructure.security.Permissions;
import edu.pucmm.cs.inventory.infrastructure.security.SystemRole;
import edu.pucmm.cs.inventory.infrastructure.web.dto.ChangeUserRoleRequestDTO;
import edu.pucmm.cs.inventory.infrastructure.web.dto.CreateUserRequestDTO;
import edu.pucmm.cs.inventory.infrastructure.web.dto.RoleResponseDTO;
import edu.pucmm.cs.inventory.infrastructure.web.dto.UserResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Controlador REST (Capa de Infraestructura Web) para la administración de
 * cuentas y roles.
 * <p>
 * Todos los endpoints exigen el permiso granular 'user:manage'. La creación y el
 * cambio de rol se delegan en {@link KeycloakAdminService}, que traduce el rol
 * del sistema a permisos y los asigna en Keycloak.
 */
@RestController
@RequestMapping("/api/v1/admin")
@Tag(name = "Administración de Usuarios", description = "Endpoints para crear cuentas y asignar roles (combinaciones de permisos). Requieren el permiso user:manage.")
public class AdminController {

    private final KeycloakAdminService keycloakAdminService;

    public AdminController(KeycloakAdminService keycloakAdminService) {
        this.keycloakAdminService = keycloakAdminService;
    }

    /**
     * Lista el catálogo de roles del sistema con los permisos que los componen,
     * para poblar el selector de rol de la interfaz de administración.
     */
    @GetMapping("/roles")
    @PreAuthorize("hasAuthority('" + Permissions.USER_MANAGE + "')")
    @Operation(summary = "Listar Roles del Sistema", description = "Devuelve los roles disponibles y el conjunto de permisos que otorga cada uno.")
    public ResponseEntity<List<RoleResponseDTO>> getRoles() {
        List<RoleResponseDTO> roles = Arrays.stream(SystemRole.values())
                .map(role -> new RoleResponseDTO(role.name(), role.getDisplayName(), role.getPermissions()))
                .toList();
        return ResponseEntity.ok(roles);
    }

    /**
     * Lista las cuentas del sistema con su rol y permisos efectivos.
     */
    @GetMapping("/users")
    @PreAuthorize("hasAuthority('" + Permissions.USER_MANAGE + "')")
    @Operation(summary = "Listar Cuentas", description = "Recupera todas las cuentas registradas en el realm con sus permisos y el rol del sistema resuelto.")
    public ResponseEntity<List<UserResponseDTO>> getUsers() {
        return ResponseEntity.ok(keycloakAdminService.listUsers());
    }

    /**
     * Crea una cuenta y le asigna un rol (conjunto de permisos).
     */
    @PostMapping("/users")
    @PreAuthorize("hasAuthority('" + Permissions.USER_MANAGE + "')")
    @Operation(summary = "Crear Cuenta", description = "Crea una cuenta en Keycloak, fija su contraseña inicial y le asigna los permisos que componen el rol indicado.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Cuenta creada con éxito")
    public ResponseEntity<UserResponseDTO> createUser(@Valid @RequestBody CreateUserRequestDTO request) {
        UserResponseDTO response = keycloakAdminService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Cambia el rol (conjunto de permisos) de una cuenta existente.
     */
    @PutMapping("/users/{id}/role")
    @PreAuthorize("hasAuthority('" + Permissions.USER_MANAGE + "')")
    @Operation(summary = "Cambiar Rol de Cuenta", description = "Reemplaza los permisos de una cuenta por los del rol indicado.")
    public ResponseEntity<UserResponseDTO> changeUserRole(
            @Parameter(description = "Identificador de la cuenta en Keycloak", required = true) @PathVariable String id,
            @Valid @RequestBody ChangeUserRoleRequestDTO request) {

        UserResponseDTO response = keycloakAdminService.changeUserRole(id, request.getRole());
        return ResponseEntity.ok(response);
    }
}
