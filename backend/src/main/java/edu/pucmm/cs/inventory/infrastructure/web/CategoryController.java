package edu.pucmm.cs.inventory.infrastructure.web;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import edu.pucmm.cs.inventory.application.CategoryService;
import edu.pucmm.cs.inventory.infrastructure.web.dto.CategoryRequestDTO;
import edu.pucmm.cs.inventory.infrastructure.web.dto.CategoryResponseDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Controlador REST (Capa de Infraestructura Web) para la gestión de Categorías.
 *
 * Expone los endpoints para listar, crear y eliminar categorías de productos.
 * Asegurado vía @PreAuthorize contra los tokens JWT emitidos por Keycloak.
 */
@RestController
@RequestMapping("/api/v1/categories")
@Tag(name = "Gestión de Categorías", description = "Endpoints para listar, crear y eliminar categorías de productos.")
public class CategoryController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    /**
     * Endpoint GET para listar todas las categorías con su cantidad de productos.
     *
     * Requiere el rol granular 'product:view'.
     */
    @GetMapping
    @PreAuthorize("hasAuthority('product:view')")
    @Operation(summary = "Listar Categorías", description = "Recupera todas las categorías registradas, ordenadas por nombre, incluyendo la cantidad de productos asociados a cada una.")
    public ResponseEntity<List<CategoryResponseDTO>> getCategories() {
        List<CategoryResponseDTO> categories = categoryService.getCategories();
        return ResponseEntity.ok(categories);
    }

    /**
     * Endpoint POST para crear una nueva categoría.
     *
     * Requiere el rol granular 'product:manage'.
     */
    @PostMapping
    @PreAuthorize("hasAuthority('product:manage')")
    @Operation(summary = "Crear una Nueva Categoría", description = "Registra una nueva categoría de productos. El nombre debe ser único.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Categoría creada con éxito")
    public ResponseEntity<CategoryResponseDTO> createCategory(
            @Valid @RequestBody CategoryRequestDTO request) {

        CategoryResponseDTO response = categoryService.createCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Endpoint DELETE para eliminar una categoría existente.
     *
     * Requiere el rol granular 'product:manage'. Falla con 409 si la categoría tiene
     * productos asociados.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('product:manage')")
    @Operation(summary = "Eliminar Categoría", description = "Elimina una categoría siempre que no tenga productos asociados; de lo contrario responde 409 Conflict.")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "Categoría eliminada exitosamente")
    public ResponseEntity<Void> deleteCategory(
            @Parameter(description = "Identificador único UUID de la categoría", required = true) @PathVariable @NonNull UUID id) {

        categoryService.deleteCategory(id);
        return ResponseEntity.noContent().build();
    }
}
