package edu.pucmm.cs.inventory.infrastructure.security;

import java.util.Set;

/**
 * Fuente única de verdad de los permisos granulares del sistema.
 * <p>
 * Cada permiso corresponde a un rol de Realm en Keycloak y se verifica en las
 * operaciones críticas mediante {@code @PreAuthorize("hasAuthority('...')")}.
 * La autorización SIEMPRE se hace por permiso, nunca por nombre de rol: los
 * "roles del sistema" (ver {@link SystemRole}) son solo combinaciones de estos
 * permisos.
 * <p>
 * Son constantes {@code static final String} para poder referenciarlas dentro
 * de las anotaciones {@code @PreAuthorize} (que exigen expresiones constantes en
 * tiempo de compilación) y así eliminar los strings mágicos duplicados.
 */
public final class Permissions {

    /** Ver catálogo de productos y categorías. */
    public static final String PRODUCT_VIEW = "product:view";
    /** Crear, editar y eliminar productos y categorías. */
    public static final String PRODUCT_MANAGE = "product:manage";
    /** Ver niveles de inventario e historial de movimientos. */
    public static final String STOCK_VIEW = "stock:view";
    /** Registrar entradas, salidas y ajustes de stock. */
    public static final String STOCK_MANAGE = "stock:manage";
    /** Acceder y exportar reportes y el dashboard. */
    public static final String REPORT_VIEW = "report:view";
    /** Gestionar usuarios, roles y permisos. */
    public static final String USER_MANAGE = "user:manage";
    /** Consultar registros de auditoría. */
    public static final String AUDIT_VIEW = "audit:view";

    /**
     * Conjunto de todos los permisos gestionados por el sistema. Sirve para
     * filtrar, entre los realm roles de un usuario en Keycloak, solo los que son
     * permisos de la aplicación (ignorando roles internos como default-roles).
     */
    public static final Set<String> ALL = Set.of(
            PRODUCT_VIEW,
            PRODUCT_MANAGE,
            STOCK_VIEW,
            STOCK_MANAGE,
            REPORT_VIEW,
            USER_MANAGE,
            AUDIT_VIEW);

    private Permissions() {
        // Clase de constantes: no debe instanciarse.
    }
}
