package edu.pucmm.cs.inventory.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Pruebas del catálogo de roles del sistema.
 * <p>
 * Verifican que cada rol es exactamente la combinación de permisos esperada
 * (requisito: "los roles del sistema se construyen asignando combinaciones de
 * permisos") y que la resolución inversa permiso→rol funciona.
 */
class SystemRoleTest {

    @Test
    @DisplayName("ADMIN concentra los 7 permisos del sistema")
    void adminTieneTodosLosPermisos() {
        assertThat(SystemRole.ADMIN.getPermissions()).containsExactlyInAnyOrder(
                Permissions.PRODUCT_VIEW,
                Permissions.PRODUCT_MANAGE,
                Permissions.STOCK_VIEW,
                Permissions.STOCK_MANAGE,
                Permissions.REPORT_VIEW,
                Permissions.USER_MANAGE,
                Permissions.AUDIT_VIEW);
    }

    @Test
    @DisplayName("VIEWER es de solo lectura: no incluye ningún permiso de gestión")
    void viewerEsSoloLectura() {
        assertThat(SystemRole.VIEWER.getPermissions())
                .containsExactlyInAnyOrder(
                        Permissions.PRODUCT_VIEW,
                        Permissions.STOCK_VIEW,
                        Permissions.REPORT_VIEW)
                .doesNotContain(
                        Permissions.PRODUCT_MANAGE,
                        Permissions.STOCK_MANAGE,
                        Permissions.USER_MANAGE);
    }

    @Test
    @DisplayName("WAREHOUSE_CLERK puede gestionar stock pero no productos ni usuarios")
    void almacenistaGestionaSoloStock() {
        Set<String> permisos = SystemRole.WAREHOUSE_CLERK.getPermissions();
        assertThat(permisos).contains(Permissions.STOCK_MANAGE);
        assertThat(permisos).doesNotContain(Permissions.PRODUCT_MANAGE, Permissions.USER_MANAGE);
    }

    @Test
    @DisplayName("Solo ADMIN concede el permiso de gestión de usuarios")
    void soloAdminGestionaUsuarios() {
        for (SystemRole role : SystemRole.values()) {
            boolean puedeGestionarUsuarios = role.getPermissions().contains(Permissions.USER_MANAGE);
            assertThat(puedeGestionarUsuarios)
                    .as("El rol %s no debería tener user:manage salvo ADMIN", role)
                    .isEqualTo(role == SystemRole.ADMIN);
        }
    }

    @Test
    @DisplayName("fromPermissions resuelve el rol cuando el conjunto de permisos coincide exactamente")
    void resuelveRolDesdePermisosExactos() {
        assertThat(SystemRole.fromPermissions(SystemRole.VIEWER.getPermissions()))
                .contains(SystemRole.VIEWER);
    }

    @Test
    @DisplayName("fromPermissions devuelve vacío para una combinación personalizada")
    void devuelveVacioParaCombinacionPersonalizada() {
        Set<String> personalizado = Set.of(Permissions.PRODUCT_VIEW, Permissions.AUDIT_VIEW);
        assertThat(SystemRole.fromPermissions(personalizado)).isEmpty();
    }
}
