package edu.pucmm.cs.inventory.infrastructure.web;

import edu.pucmm.cs.inventory.application.ProductAuditService;
import edu.pucmm.cs.inventory.application.StockMovementAuditService;
import edu.pucmm.cs.inventory.infrastructure.security.Permissions;
import edu.pucmm.cs.inventory.infrastructure.web.dto.ProductAuditResponseDTO;
import edu.pucmm.cs.inventory.infrastructure.web.dto.StockMovementAuditResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Controlador REST para consultar la auditoría del sistema (DevSecOps / Compliance).
 */
@RestController
@RequestMapping("/api/v1/audit")
@Tag(name = "Auditoría del Sistema", description = "Endpoints para consultar el historial inmutable de cambios (Hibernate Envers).")
public class AuditController {

    private final ProductAuditService productAuditService;
    private final StockMovementAuditService stockMovementAuditService;

    public AuditController(ProductAuditService productAuditService, StockMovementAuditService stockMovementAuditService) {
        this.productAuditService = productAuditService;
        this.stockMovementAuditService = stockMovementAuditService;
    }

    @GetMapping("/products")
    @PreAuthorize("hasAuthority('" + Permissions.AUDIT_VIEW + "')")
    @Operation(summary = "Auditoría global de Productos", description = "Lista todas las revisiones de productos ordenadas por fecha descendente.")
    public ResponseEntity<List<ProductAuditResponseDTO>> getAllProductAuditHistory() {
        return ResponseEntity.ok(productAuditService.getAllProductAuditHistory());
    }

    @GetMapping("/stock-movements")
    @PreAuthorize("hasAuthority('" + Permissions.AUDIT_VIEW + "')")
    @Operation(summary = "Auditoría global de Movimientos de Stock", description = "Lista todas las revisiones de movimientos de stock ordenadas por fecha descendente.")
    public ResponseEntity<List<StockMovementAuditResponseDTO>> getAllStockMovementAuditHistory() {
        return ResponseEntity.ok(stockMovementAuditService.getAllStockMovementAuditHistory());
    }
}
