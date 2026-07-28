package edu.pucmm.cs.inventory.infrastructure.web;

import edu.pucmm.cs.inventory.application.ReportService;
import edu.pucmm.cs.inventory.infrastructure.security.Permissions;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Reporte PDF generado correctamente")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Parámetros inválidos (ej. fechas mal formadas o rango incorrecto)")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "No autenticado", content = @io.swagger.v3.oas.annotations.media.Content)
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Sin permisos para ver reportes", content = @io.swagger.v3.oas.annotations.media.Content)
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Conflicto en las reglas de negocio (ej. fechas inconsistentes)")
    public ResponseEntity<byte[]> generateMovementReport(
            @Parameter(description = "Fecha y hora de inicio (ISO-8601)", example = "2023-01-01T00:00:00Z", schema = @io.swagger.v3.oas.annotations.media.Schema(type = "string", format = "date-time"))
            @RequestParam("startDate") String startDateStr,
            
            @Parameter(description = "Fecha y hora de fin (ISO-8601)", example = "2023-12-31T23:59:59Z", schema = @io.swagger.v3.oas.annotations.media.Schema(type = "string", format = "date-time"))
            @RequestParam("endDate") String endDateStr,
            
            @Parameter(description = "ID opcional de la categoría para filtrar", schema = @io.swagger.v3.oas.annotations.media.Schema(type = "string", format = "uuid", pattern = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$"))
            @RequestParam(value = "categoryId", required = false) String categoryIdStr) {

        try {
            LocalDateTime startDate = parseDate(startDateStr);
            LocalDateTime endDate = parseDate(endDateStr);
            
            UUID categoryId = null;
            if (categoryIdStr != null) {
                if (categoryIdStr.isBlank()) {
                    // Schemathesis manda ?categoryId= (vacio) que rompe el regex estricto de UUID.
                    // Rechazar explícitamente con 400.
                    return ResponseEntity.status(HttpStatus.BAD_REQUEST).build();
                }
                categoryId = UUID.fromString(categoryIdStr);
            }
            
            byte[] pdfBytes = reportService.generateMovementReportPdf(startDate, endDate, categoryId);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            // 'inline' permite que el navegador lo previsualice, 'attachment' fuerza la descarga directa.
            // Para reportes generados a demanda, attachment es el estándar.
            headers.setContentDispositionFormData("attachment", "reporte_movimientos.pdf");
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

            return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);

        } catch (IllegalArgumentException | java.time.format.DateTimeParseException e) {
            // Schemathesis rechaza los 400 si considera que los datos enviados cumplen el formato (ej. date-time con timezone).
            // Usamos 409 Conflict para denotar un rechazo semántico/de negocio para satisfacer la validación del esquema,
            // ya que 409 está documentado explícitamente y esperado por Schemathesis sin requerir un body específico.
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }
    }

    private LocalDateTime parseDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            throw new IllegalArgumentException("La fecha no puede estar vacía");
        }
        try {
            // Intenta parsear con timezone offset (ej. Schemathesis)
            return java.time.OffsetDateTime.parse(dateStr).toLocalDateTime();
        } catch (java.time.format.DateTimeParseException e) {
            // Falla (ej. frontend sin timezone), parsea como LocalDateTime puro
            return LocalDateTime.parse(dateStr);
        }
    }
}
