package edu.pucmm.cs.inventory.infrastructure.security;

import java.util.Arrays;
import java.util.Optional;
import java.util.Set;

/**
 * Catálogo de roles del sistema.
 * <p>
 * Un rol es únicamente una <b>combinación de permisos</b> (ver
 * {@link Permissions}) pensada para simplificar la asignación cuando el
 * administrador crea una cuenta. Es una etiqueta de conveniencia: al asignar un
 * rol, el backend concede al usuario en Keycloak SOLO los permisos granulares
 * que lo componen, y las operaciones críticas se autorizan por permiso, nunca
 * por el nombre del rol.
 */
public enum SystemRole {

    /** Acceso total: todos los permisos del sistema. */
    ADMIN("Administrador", Set.of(
            Permissions.PRODUCT_VIEW,
            Permissions.PRODUCT_MANAGE,
            Permissions.STOCK_VIEW,
            Permissions.STOCK_MANAGE,
            Permissions.REPORT_VIEW,
            Permissions.USER_MANAGE,
            Permissions.AUDIT_VIEW)),

    /** Gestiona productos e inventario y consulta reportes. */
    INVENTORY_MANAGER("Gerente de Inventario", Set.of(
            Permissions.PRODUCT_VIEW,
            Permissions.PRODUCT_MANAGE,
            Permissions.STOCK_VIEW,
            Permissions.STOCK_MANAGE,
            Permissions.REPORT_VIEW)),

    /** Opera el almacén: consulta productos y registra movimientos de stock. */
    WAREHOUSE_CLERK("Almacenista", Set.of(
            Permissions.PRODUCT_VIEW,
            Permissions.STOCK_VIEW,
            Permissions.STOCK_MANAGE)),

    /** Solo lectura: catálogo, inventario y reportes. */
    VIEWER("Consulta", Set.of(
            Permissions.PRODUCT_VIEW,
            Permissions.STOCK_VIEW,
            Permissions.REPORT_VIEW)),

    /** Auditoría: consulta registros de auditoría y reportes. */
    AUDITOR("Auditor", Set.of(
            Permissions.REPORT_VIEW,
            Permissions.AUDIT_VIEW));

    private final String displayName;
    private final Set<String> permissions;

    SystemRole(String displayName, Set<String> permissions) {
        this.displayName = displayName;
        this.permissions = permissions;
    }

    /** Nombre legible para mostrar en la interfaz de administración. */
    public String getDisplayName() {
        return displayName;
    }

    /** Conjunto de permisos granulares que componen el rol. */
    public Set<String> getPermissions() {
        return permissions;
    }

    /**
     * Resuelve un {@link SystemRole} a partir del conjunto de permisos que tiene
     * asignado un usuario en Keycloak. Permite mostrar en la UI a qué rol
     * corresponde un usuario existente. Si los permisos no coinciden exactamente
     * con ningún rol del catálogo, devuelve vacío (asignación personalizada).
     *
     * @param permissions permisos actuales del usuario
     * @return el rol cuyo conjunto de permisos coincide exactamente, si existe
     */
    public static Optional<SystemRole> fromPermissions(Set<String> permissions) {
        return Arrays.stream(values())
                .filter(role -> role.permissions.equals(permissions))
                .findFirst();
    }
}
