package edu.pucmm.cs.inventory.infrastructure.web;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import edu.pucmm.cs.inventory.application.ProductAuditService;
import edu.pucmm.cs.inventory.application.ProductService;
import edu.pucmm.cs.inventory.infrastructure.web.dto.ProductAuditResponseDTO;
import edu.pucmm.cs.inventory.infrastructure.web.dto.ProductRequestDTO;
import edu.pucmm.cs.inventory.infrastructure.web.dto.ProductResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Controlador REST (Capa de Infraestructura Web) para el mantenimiento de
 * Productos.
 * 
 * Expone los endpoints de la API hacia clientes externos (Frontend, Apps).
 * Se encarga del enrutamiento HTTP, validación estructural de entrada y
 * delegación de la lógica comercial al 'ProductService'.
 * Está asegurado vía @PreAuthorize para validar tokens JWT emitidos por
 * Keycloak.
 */
@RestController
@RequestMapping("/api/v1/products")
@Tag(name = "Gestión de Productos", description = "Endpoints para el CRUD estándar y listado de catálogo de productos.")
public class ProductController {

    private final ProductService productService;
    private final ProductAuditService productAuditService;

    public ProductController(ProductService productService, ProductAuditService productAuditService) {
        this.productService = productService;
        this.productAuditService = productAuditService;
    }

    /**
     * Endpoint GET para listar y paginar los productos.
     * 
     * Requiere el rol granular 'product:view' en el token de Keycloak.
     */
    @GetMapping
    @PreAuthorize("hasAuthority('product:view')")
    @Operation(summary = "Consultar Catálogo", description = "Recupera una lista paginada de todos los productos registrados. Admite un término de búsqueda opcional (?search=) que filtra por nombre o código SKU, además de filtros y ordenamiento mediante el objeto Pageable.")
    public ResponseEntity<Page<ProductResponseDTO>> getProducts(
            @Parameter(description = "Término de búsqueda opcional que filtra por nombre o código SKU (case-insensitive).") @RequestParam(required = false) String search,
            @Parameter(description = "Inyección automática de Spring. Parámetros de URL soportados: ?page=0&size=10&sort=name,asc", hidden = true) @NonNull Pageable pageable) {

        Page<ProductResponseDTO> products = productService.getProducts(search, pageable);
        return ResponseEntity.ok(products);
    }

    /**
     * Endpoint GET para consultar alertas de stock crítico
     * 
     * Requiere el rol granular 'report:view' (compatible con conversor Keycloak).
     */
    @GetMapping("/alerts/critical-stock")
    @PreAuthorize("hasAuthority('report:view')")
    @Operation(summary = "Alertas de stock crítico", description = "Calcula el stock al vuelo sumando el historial inmutable de movimientos para devolver productos cuyo stock actual es menor o igual a su stock mínimo, evitando condiciones de carrera.")
    public ResponseEntity<List<ProductResponseDTO>> getCriticalStockAlerts() {
        List<ProductResponseDTO> alerts = productService.getCriticalStockAlerts();
        return ResponseEntity.ok(alerts);
    }

    /**
     * Endpoint GET para consultar el historial de auditoría (Hibernate Envers) de un producto.
     *
     * Requiere el rol granular 'report:view' (consulta de solo lectura / auditoría).
     */
    @GetMapping("/{id}/audit")
    @PreAuthorize("hasAuthority('report:view')")
    @Operation(summary = "Historial de auditoría del producto", description = "Devuelve las revisiones registradas por Hibernate Envers para un producto (alta, modificaciones y baja), ordenadas de la más reciente a la más antigua, con la foto de sus datos en cada cambio. Nota: Envers registra qué cambió y cuándo, no el usuario.")
    public ResponseEntity<List<ProductAuditResponseDTO>> getProductAuditHistory(
            @Parameter(description = "Identificador único UUID del producto", required = true) @PathVariable @NonNull UUID id) {

        List<ProductAuditResponseDTO> history = productAuditService.getProductAuditHistory(id);
        return ResponseEntity.ok(history);
    }

    /**
     * Endpoint POST para insertar nuevos productos al sistema.
     * 
     * Requiere el rol granular 'product:manage'.
     */
    @PostMapping
    @PreAuthorize("hasAuthority('product:manage')")
    @Operation(summary = "Crear un Nuevo Producto", description = "Registra un producto e inicializa su cantidad de stock generando una entrada automática en el historial (StockMovement).")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Producto creado con éxito")
    public ResponseEntity<ProductResponseDTO> createProduct(
            @Valid @RequestBody ProductRequestDTO request) {

        ProductResponseDTO response = productService.createProduct(request);
        // Devuelve código HTTP 201 Created indicando el éxito de la inserción
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Endpoint PUT para actualizar en bloque los datos de un producto.
     * 
     * Requiere el rol granular 'product:manage'.
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('product:manage')")
    @Operation(summary = "Actualizar Información de Producto", description = "Reescribe los metadatos y configuración descriptiva de un producto pre-existente.")
    public ResponseEntity<ProductResponseDTO> updateProduct(
            @Parameter(description = "Identificador único UUID del producto", required = true) @PathVariable @NonNull UUID id,
            @Valid @RequestBody ProductRequestDTO request) {

        ProductResponseDTO response = productService.updateProduct(id, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint DELETE para suprimir de forma permanente un producto.
     * 
     * Requiere el rol granular 'product:manage'.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('product:manage')")
    @Operation(summary = "Eliminar Producto Definitivamente", description = "Borra físicamente (Hard Delete) el registro de un producto. Si cuenta con movimientos de stock, fallará por integridad referencial.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Producto eliminado exitosamente")
    public ResponseEntity<Void> deleteProduct(
            @Parameter(description = "Identificador único UUID del producto a destruir", required = true) @PathVariable @NonNull UUID id) {

        productService.deleteProduct(id);
        // Devuelve HTTP 204 No Content confirmando la eliminación sin retornar cuerpo
        return ResponseEntity.noContent().build();
    }
}
