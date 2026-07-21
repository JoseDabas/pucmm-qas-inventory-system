package edu.pucmm.cs.inventory.infrastructure.web;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.pucmm.cs.inventory.application.StockMovementService;
import edu.pucmm.cs.inventory.infrastructure.web.dto.StockMovementRequestDTO;
import edu.pucmm.cs.inventory.infrastructure.web.dto.StockMovementResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Controlador REST (Capa de Infraestructura Web) para el Historial de Movimientos.
 *
 * Expone los endpoints para consultar el historial de entradas y salidas de stock
 * y para registrar nuevos movimientos. Asegurado vía @PreAuthorize contra los tokens
 * JWT emitidos por Keycloak.
 */
@RestController
@RequestMapping("/api/v1/stock-movements")
@Tag(name = "Historial de Movimientos", description = "Endpoints para consultar y registrar entradas y salidas de stock.")
public class StockMovementController {

    private final StockMovementService stockMovementService;

    public StockMovementController(StockMovementService stockMovementService) {
        this.stockMovementService = stockMovementService;
    }

    /**
     * Endpoint GET para listar y paginar el historial de movimientos.
     *
     * Requiere el rol granular 'report:view' (consulta de solo lectura / auditoría).
     */
    @GetMapping
    @PreAuthorize("hasAuthority('report:view')")
    @Operation(summary = "Consultar Historial de Movimientos", description = "Recupera una lista paginada de todas las entradas y salidas de stock, ordenadas por fecha descendente. Admite un término de búsqueda opcional (?search=) que filtra por nombre de producto o usuario.")
    public ResponseEntity<Page<StockMovementResponseDTO>> getMovements(
            @Parameter(description = "Término de búsqueda opcional que filtra por nombre de producto o usuario (case-insensitive).") @RequestParam(required = false) String search,
            @Parameter(description = "Inyección automática de Spring. Parámetros de URL soportados: ?page=0&size=10&sort=date,desc", hidden = true) @NonNull Pageable pageable) {

        Page<StockMovementResponseDTO> movements = stockMovementService.getMovements(search, pageable);
        return ResponseEntity.ok(movements);
    }

    /**
     * Endpoint POST para registrar una entrada o salida de stock.
     *
     * Requiere el rol granular 'product:manage' (misma autoridad que modifica inventario).
     */
    @PostMapping
    @PreAuthorize("hasAuthority('product:manage')")
    @Operation(summary = "Registrar Movimiento de Stock", description = "Registra una entrada (IN) o salida (OUT) de stock para un producto, calculando la cantidad anterior y nueva. Rechaza salidas que dejarían el inventario en negativo.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Movimiento registrado con éxito")
    public ResponseEntity<StockMovementResponseDTO> registerMovement(
            @Valid @RequestBody StockMovementRequestDTO request) {

        StockMovementResponseDTO response = stockMovementService.registerMovement(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }
}
