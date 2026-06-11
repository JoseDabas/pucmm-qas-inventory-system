package edu.pucmm.cs.inventory.infrastructure.web;

import edu.pucmm.cs.inventory.application.ProductService;
import edu.pucmm.cs.inventory.infrastructure.web.dto.ProductRequestDTO;
import edu.pucmm.cs.inventory.infrastructure.web.dto.ProductResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.lang.NonNull;

import java.util.UUID;

/**
 * Controlador REST (Capa de Infraestructura Web) para el mantenimiento de Productos.
 * 
 * Expone los endpoints de la API hacia clientes externos (Frontend, Apps).
 * Se encarga del enrutamiento HTTP, validación estructural de entrada y 
 * delegación de la lógica comercial al 'ProductService'.
 * Está asegurado vía @PreAuthorize para validar tokens JWT emitidos por Keycloak.
 */
@RestController
@RequestMapping("/api/v1/products")
@Tag(name = "Gestión de Productos", description = "Endpoints para el CRUD estándar y listado de catálogo de productos.")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    /**
     * Endpoint GET para listar y paginar los productos.
     * 
     * Requiere el rol granular 'product:view' en el token de Keycloak.
     */
    @GetMapping
    @PreAuthorize("hasRole('product:view')")
    @Operation(summary = "Consultar Catálogo", description = "Recupera una lista paginada de todos los productos registrados. Soporta filtros y ordenamiento mediante el objeto Pageable.")
    public ResponseEntity<Page<ProductResponseDTO>> getProducts(
            @Parameter(description = "Inyección automática de Spring. Parámetros de URL soportados: ?page=0&size=10&sort=name,asc", hidden = true)
            @NonNull Pageable pageable) {
        
        Page<ProductResponseDTO> products = productService.getProducts(pageable);
        return ResponseEntity.ok(products);
    }

    /**
     * Endpoint POST para insertar nuevos productos al sistema.
     * 
     * Requiere el rol granular 'product:manage'.
     */
    @PostMapping
    @PreAuthorize("hasRole('product:manage')")
    @Operation(summary = "Crear un Nuevo Producto", description = "Registra un producto e inicializa su cantidad de stock generando una entrada automática en el historial (StockMovement).")
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
    @PreAuthorize("hasRole('product:manage')")
    @Operation(summary = "Actualizar Información de Producto", description = "Reescribe los metadatos y configuración descriptiva de un producto pre-existente.")
    public ResponseEntity<ProductResponseDTO> updateProduct(
            @Parameter(description = "Identificador único UUID del producto", required = true)
            @PathVariable @NonNull UUID id,
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
    @PreAuthorize("hasRole('product:manage')")
    @Operation(summary = "Eliminar Producto Definitivamente", description = "Borra físicamente (Hard Delete) el registro de un producto. Si cuenta con movimientos de stock, fallará por integridad referencial.")
    public ResponseEntity<Void> deleteProduct(
            @Parameter(description = "Identificador único UUID del producto a destruir", required = true)
            @PathVariable @NonNull UUID id) {
        
        productService.deleteProduct(id);
        // Devuelve HTTP 204 No Content confirmando la eliminación sin retornar cuerpo
        return ResponseEntity.noContent().build();
    }
}
