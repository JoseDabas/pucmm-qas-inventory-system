package edu.pucmm.cs.inventory.infrastructure.web;

import edu.pucmm.cs.inventory.application.ReportService;
import edu.pucmm.cs.inventory.infrastructure.security.Permissions;
import edu.pucmm.cs.inventory.infrastructure.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReportController.class)
@Import(SecurityConfig.class)
class ReportControllerApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ReportService reportService;

    private String startDateStr = "2023-01-01T00:00:00";
    private String endDateStr = "2023-12-31T23:59:59";

    @Test
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

    @Test
    void testGenerateMovementReport_IllegalArgumentException_ReturnsBadRequest() throws Exception {
        when(reportService.generateMovementReportPdf(any(), any(), any())).thenThrow(new IllegalArgumentException("Fechas inválidas"));

        mockMvc.perform(get("/api/v1/reports/movements")
                .with(jwt().authorities(new SimpleGrantedAuthority(Permissions.REPORT_VIEW)))
                .param("startDate", startDateStr)
                .param("endDate", endDateStr))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testGenerateMovementReport_WithoutPermission_ReturnsForbidden() throws Exception {
        mockMvc.perform(get("/api/v1/reports/movements")
                .with(jwt().authorities(new SimpleGrantedAuthority("OTHER_PERMISSION")))
                .param("startDate", startDateStr)
                .param("endDate", endDateStr))
                .andExpect(status().isForbidden());
    }

    @Test
    void testGenerateMovementReport_WithoutAuth_ReturnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/reports/movements")
                .param("startDate", startDateStr)
                .param("endDate", endDateStr))
                .andExpect(status().isUnauthorized());
    }
}
