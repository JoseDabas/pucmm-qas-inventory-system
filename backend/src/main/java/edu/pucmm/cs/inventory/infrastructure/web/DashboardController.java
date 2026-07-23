package edu.pucmm.cs.inventory.infrastructure.web;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.pucmm.cs.inventory.application.DashboardService;
import edu.pucmm.cs.inventory.infrastructure.web.dto.DashboardMetricsResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Controlador REST del tablero de control.
 * <p>
 * Expone las métricas e indicadores agregados del inventario. A diferencia del
 * resto de endpoints, NO exige un permiso granular: basta con estar autenticado
 * (regla por defecto de SecurityConfig), de modo que cualquier rol pueda ver los
 * valores del dashboard. Las listas detalladas siguen protegidas en sus
 * respectivos controllers.
 */
@RestController
@RequestMapping("/api/v1/dashboard")
@Tag(name = "Tablero de Control", description = "Métricas e indicadores agregados del inventario, visibles para cualquier usuario autenticado.")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping("/metrics")
    @Operation(summary = "Métricas del Dashboard", description = "Devuelve las métricas e indicadores agregados (productos, categorías, movimientos, críticos, unidades y valor del inventario). Accesible para cualquier usuario autenticado.")
    public ResponseEntity<DashboardMetricsResponseDTO> getMetrics() {
        return ResponseEntity.ok(dashboardService.getMetrics());
    }
}
