package edu.pucmm.cs.inventory.infrastructure.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * Pruebas de la fuente única de permisos.
 * Verifican que {@link Permissions#ALL} agrupe exactamente los 7 permisos del
 * sistema (conjunto que se usa para filtrar los realm roles gestionados).
 */
class PermissionsTest {

    @Test
    @DisplayName("ALL contiene exactamente los 7 permisos del sistema")
    void allContieneLosSietePermisos() {
        assertThat(Permissions.ALL).containsExactlyInAnyOrder(
                Permissions.PRODUCT_VIEW,
                Permissions.PRODUCT_MANAGE,
                Permissions.STOCK_VIEW,
                Permissions.STOCK_MANAGE,
                Permissions.REPORT_VIEW,
                Permissions.USER_MANAGE,
                Permissions.AUDIT_VIEW);
    }

    @Test
    @DisplayName("ALL tiene tamaño 7")
    void allTieneTamanoSiete() {
        assertThat(Permissions.ALL).hasSize(7);
    }
}
