package edu.pucmm.cs.inventory.infrastructure.web;

import edu.pucmm.cs.inventory.application.ProductAuditService;
import edu.pucmm.cs.inventory.application.StockMovementAuditService;
import edu.pucmm.cs.inventory.infrastructure.security.Permissions;
import edu.pucmm.cs.inventory.infrastructure.security.SecurityConfig;
import edu.pucmm.cs.inventory.infrastructure.web.dto.ProductAuditResponseDTO;
import edu.pucmm.cs.inventory.infrastructure.web.dto.StockMovementAuditResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de API (slice WebMvcTest) para AuditController.
 * Verifican el enrutamiento, la correcta serialización de las respuestas
 * y que todas las operaciones exijan el permiso 'audit:view'. 
 * Los servicios de auditoría (Envers) se sustituyen por mocks.
 */
@WebMvcTest(AuditController.class)
@Import(SecurityConfig.class)
class AuditControllerApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductAuditService productAuditService;

    @MockitoBean
    private StockMovementAuditService stockMovementAuditService;

    /**
     * Valida que un usuario con privilegios de auditoría pueda extraer
     * el historial completo de cambios estructurales sobre los productos.
     */
    @Test
    @DisplayName("GET auditoría de productos con audit:view devuelve 200 y el historial")
    void testGetAllProductAuditHistory_Success() throws Exception {
        UUID productId = UUID.randomUUID();
        ProductAuditResponseDTO dto = new ProductAuditResponseDTO();
        dto.setEntityId(productId);
        dto.setRevisionType("ADD");

        when(productAuditService.getAllProductAuditHistory()).thenReturn(Collections.singletonList(dto));

        mockMvc.perform(get("/api/v1/audit/products")
                .with(jwt().authorities(new SimpleGrantedAuthority(Permissions.AUDIT_VIEW))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].entityId").value(productId.toString()))
                .andExpect(jsonPath("$[0].revisionType").value("ADD"));
    }

    /**
     * Asegura que el endpoint de productos audite la autorización,
     * denegando el acceso (HTTP 403) a usuarios sin el permiso audit:view.
     */
    @Test
    @DisplayName("GET auditoría de productos con permiso incorrecto devuelve 403")
    void testGetAllProductAuditHistory_WithoutPermission_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/audit/products")
                .with(jwt().authorities(new SimpleGrantedAuthority("OTHER_PERMISSION"))))
                .andExpect(status().isForbidden());
    }

    /**
     * Comprueba que los supervisores o auditores puedan consultar el ledger histórico
     * de entradas y salidas de mercancía, verificando la estructura JSON de salida.
     */
    @Test
    @DisplayName("GET auditoría de movimientos con audit:view devuelve 200 y el historial")
    void testGetAllStockMovementAuditHistory_Success() throws Exception {
        UUID movId = UUID.randomUUID();
        StockMovementAuditResponseDTO dto = new StockMovementAuditResponseDTO();
        dto.setEntityId(movId);
        dto.setRevisionType("MOD");

        when(stockMovementAuditService.getAllStockMovementAuditHistory()).thenReturn(Collections.singletonList(dto));

        mockMvc.perform(get("/api/v1/audit/stock-movements")
                .with(jwt().authorities(new SimpleGrantedAuthority(Permissions.AUDIT_VIEW))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].entityId").value(movId.toString()))
                .andExpect(jsonPath("$[0].revisionType").value("MOD"));
    }

    /**
     * Protege el endpoint de historial de movimientos contra peticiones
     * completamente anónimas, forzando un estado HTTP 401.
     */
    @Test
    @DisplayName("GET auditoría de movimientos sin token devuelve 401")
    void testGetAllStockMovementAuditHistory_WithoutAuth_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/audit/stock-movements"))
                .andExpect(status().isUnauthorized());
    }
}
