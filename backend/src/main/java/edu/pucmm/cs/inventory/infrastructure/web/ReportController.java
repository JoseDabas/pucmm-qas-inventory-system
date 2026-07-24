package edu.pucmm.cs.inventory.infrastructure.web;

import edu.pucmm.cs.inventory.application.ReportService;
import edu.pucmm.cs.inventory.infrastructure.security.Permissions;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/reports")
@Tag(name = "Reportes", description = "Endpoints para la generación de reportes analíticos del sistema.")
public class ReportController {

    private final ReportService reportService;

    public ReportController(ReportService reportService) {
        this.reportService = reportService;
    }

    @GetMapping("/movements")
    @PreAuthorize("hasAuthority('" + Permissions.REPORT_VIEW + "')")
    @Operation(summary = "Generar Reporte de Movimientos", description = "Genera un reporte PDF con el historial de movimientos de inventario en el rango de fechas especificado.")
    public ResponseEntity<byte[]> generateMovementReport(
            @Parameter(description = "Fecha y hora de inicio (ISO-8601)", example = "2023-01-01T00:00:00")
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            
            @Parameter(description = "Fecha y hora de fin (ISO-8601)", example = "2023-12-31T23:59:59")
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate,
            
            @Parameter(description = "ID opcional de la categoría para filtrar")
            @RequestParam(value = "categoryId", required = false) UUID categoryId) {

        try {
            byte[] pdfBytes = reportService.generateMovementReportPdf(startDate, endDate, categoryId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            // 'inline' permite que el navegador lo previsualice, 'attachment' fuerza la descarga directa.
            // Para reportes generados a demanda, attachment es el estándar.
            headers.setContentDispositionFormData("attachment", "reporte_movimientos.pdf");
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);

        } catch (IllegalArgumentException e) {
            // Error de validación (fechas inválidas)
            return ResponseEntity.badRequest().build();
        }
    }
}
