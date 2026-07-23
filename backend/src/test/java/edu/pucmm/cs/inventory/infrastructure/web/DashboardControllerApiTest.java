package edu.pucmm.cs.inventory.infrastructure.web;

import edu.pucmm.cs.inventory.application.DashboardService;
import edu.pucmm.cs.inventory.infrastructure.security.SecurityConfig;
import edu.pucmm.cs.inventory.infrastructure.web.dto.DashboardMetricsResponseDTO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.math.BigDecimal;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de API (slice @WebMvcTest) para {@link DashboardController}.
 * Verifican que las métricas exigen autenticación pero NO un permiso concreto,
 * de modo que cualquier rol autenticado pueda verlas.
 */
@WebMvcTest(DashboardController.class)
@Import(SecurityConfig.class)
class DashboardControllerApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private DashboardService dashboardService;

    private DashboardMetricsResponseDTO sampleMetrics;

    @BeforeEach
    void setUp() {
        sampleMetrics = new DashboardMetricsResponseDTO();
        sampleMetrics.setTotalProducts(10);
        sampleMetrics.setTotalUnits(120);
        sampleMetrics.setTotalValue(new BigDecimal("2500.00"));
        sampleMetrics.setTotalCategories(4);
        sampleMetrics.setTotalMovements(37);
        sampleMetrics.setCriticalStockCount(2);
    }

    private RequestPostProcessor jwtWith(String authority) {
        return jwt().authorities(new SimpleGrantedAuthority(authority));
    }

    @Test
    @DisplayName("GET métricas sin token devuelve 401")
    void getMetricasSinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/api/v1/dashboard/metrics"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET métricas con cualquier rol autenticado devuelve 200 con los valores")
    void getMetricasConCualquierRolDevuelve200() throws Exception {
        when(dashboardService.getMetrics()).thenReturn(sampleMetrics);
        // 'audit:view' es un permiso que NO da acceso a productos/stock; aun así el
        // dashboard debe responder, demostrando que no exige un permiso concreto.
        mockMvc.perform(get("/api/v1/dashboard/metrics").with(jwtWith("audit:view")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalProducts").value(10))
                .andExpect(jsonPath("$.criticalStockCount").value(2));
    }
}
