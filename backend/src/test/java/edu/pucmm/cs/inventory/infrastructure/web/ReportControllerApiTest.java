package edu.pucmm.cs.inventory.infrastructure.web;

import edu.pucmm.cs.inventory.application.ReportService;
import edu.pucmm.cs.inventory.infrastructure.security.Permissions;
import edu.pucmm.cs.inventory.infrastructure.security.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pruebas de API (slice WebMvcTest) para ReportController.
 * Verifica la descarga de archivos (streaming de binarios),
 * autorizaciones y la gestión de excepciones en fechas de corte.
 */
@WebMvcTest(ReportController.class)
@Import(SecurityConfig.class)
class ReportControllerApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReportService reportService;

    private String startDateStr = "2023-01-01T00:00:00";
    private String endDateStr = "2023-12-31T23:59:59";

    /**
     * Valida que un usuario autorizado reciba un flujo binario etiquetado
     * como APPLICATION_PDF con las cabeceras Content-Disposition adecuadas
     * para forzar la descarga del reporte.
     */
    @Test
    @DisplayName("Descarga exitosa del PDF con cabeceras de adjunto (200 OK)")
    void testGenerateMovementReport_Success() throws Exception {
        byte[] pdfBytes = new byte[]{1, 2, 3};
        when(reportService.generateMovementReportPdf(any(), any(), any())).thenReturn(pdfBytes);

        mockMvc.perform(get("/api/v1/reports/movements")
                .with(jwt().authorities(new SimpleGrantedAuthority(Permissions.REPORT_VIEW)))
                .param("startDate", startDateStr)
                .param("endDate", endDateStr)
                .param("categoryId", UUID.randomUUID().toString()))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF))
                .andExpect(header().string("Content-Disposition", "form-data; name=\"attachment\"; filename=\"reporte_movimientos.pdf\""))
                .andExpect(content().bytes(pdfBytes));
    }

    /**
     * Comprueba que si las fechas proporcionadas son cronológicamente inversas
     * o inválidas, el controlador traduzca la excepción de negocio a HTTP 409.
     */
    @Test
    @DisplayName("Petición de reporte con fechas inconsistentes devuelve 409 Conflict")
    void testGenerateMovementReport_IllegalArgumentException_ReturnsConflict() throws Exception {
        when(reportService.generateMovementReportPdf(any(), any(), any())).thenThrow(new IllegalArgumentException("Fechas inválidas"));

        mockMvc.perform(get("/api/v1/reports/movements")
                .with(jwt().authorities(new SimpleGrantedAuthority(Permissions.REPORT_VIEW)))
                .param("startDate", startDateStr)
                .param("endDate", endDateStr))
                .andExpect(status().isConflict());
    }

    /**
     * Asegura la protección de datos estratégicos (movimientos en el tiempo),
     * denegando acceso a usuarios sin el rol específico de reportería.
     */
    @Test
    @DisplayName("Petición de reporte sin privilegios devuelve 403 Forbidden")
    void testGenerateMovementReport_WithoutPermission_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/reports/movements")
                .with(jwt().authorities(new SimpleGrantedAuthority("OTHER_PERMISSION")))
                .param("startDate", startDateStr)
                .param("endDate", endDateStr))
                .andExpect(status().isForbidden());
    }

    /**
     * Obliga a la negociación OAuth2 antes de intentar procesar 
     * parámetros complejos o extraer datos de la base.
     */
    @Test
    @DisplayName("Petición anónima de reporte devuelve 401 Unauthorized")
    void testGenerateMovementReport_WithoutAuth_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/reports/movements")
                .param("startDate", startDateStr)
                .param("endDate", endDateStr))
                .andExpect(status().isUnauthorized());
    }
}
