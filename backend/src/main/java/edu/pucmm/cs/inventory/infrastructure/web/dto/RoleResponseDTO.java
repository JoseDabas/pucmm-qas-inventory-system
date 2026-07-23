package edu.pucmm.cs.inventory.infrastructure.web.dto;

import java.util.Set;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO de salida que describe un rol del sistema y los permisos que lo componen.
 * Alimenta el selector de rol de la UI de administración.
 */
@Schema(description = "Rol del sistema como combinación de permisos.")
public class RoleResponseDTO {

    @Schema(description = "Identificador del rol (nombre del enum)", example = "INVENTORY_MANAGER")
    private String name;

    @Schema(description = "Nombre legible para mostrar", example = "Gerente de Inventario")
    private String displayName;

    @Schema(description = "Permisos granulares que otorga el rol")
    private Set<String> permissions;

    public RoleResponseDTO(String name, String displayName, Set<String> permissions) {
        this.name = name;
        this.displayName = displayName;
        this.permissions = permissions;
    }

    public String getName() { return name; }
    public String getDisplayName() { return displayName; }
    public Set<String> getPermissions() { return permissions; }
}
