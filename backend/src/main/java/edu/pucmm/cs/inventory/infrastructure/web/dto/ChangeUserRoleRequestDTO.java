package edu.pucmm.cs.inventory.infrastructure.web.dto;

import edu.pucmm.cs.inventory.infrastructure.security.SystemRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * DTO de entrada para cambiar el rol (conjunto de permisos) de una cuenta
 * existente.
 */
@Schema(description = "Nuevo rol del sistema a asignar a una cuenta existente.")
public class ChangeUserRoleRequestDTO {

    @Schema(description = "Rol del sistema a asignar (reemplaza los permisos actuales)", example = "INVENTORY_MANAGER", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "El rol es obligatorio")
    private SystemRole role;

    public SystemRole getRole() { return role; }

    public void setRole(SystemRole role) { this.role = role; }
}
