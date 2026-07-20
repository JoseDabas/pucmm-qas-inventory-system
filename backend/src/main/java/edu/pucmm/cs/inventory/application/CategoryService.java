package edu.pucmm.cs.inventory.application;

import edu.pucmm.cs.inventory.domain.Category;
import edu.pucmm.cs.inventory.infrastructure.persistence.repository.CategoryJpaRepository;
import edu.pucmm.cs.inventory.infrastructure.persistence.repository.CategoryProductCountView;
import edu.pucmm.cs.inventory.infrastructure.persistence.repository.ProductJpaRepository;
import edu.pucmm.cs.inventory.infrastructure.web.dto.CategoryRequestDTO;
import edu.pucmm.cs.inventory.infrastructure.web.dto.CategoryResponseDTO;
import edu.pucmm.cs.inventory.infrastructure.web.exception.CategoryInUseException;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.data.domain.Sort;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Servicio de Aplicación (Caso de Uso) para la gestión de Categorías.
 *
 * Orquesta el listado de categorías con su cantidad de productos, la creación y la
 * eliminación, apoyándose en los repositorios de categorías y productos.
 */
@Service
public class CategoryService {

    private final CategoryJpaRepository categoryRepository;
    private final ProductJpaRepository productRepository;

    public CategoryService(CategoryJpaRepository categoryRepository, ProductJpaRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    /**
     * Lista todas las categorías ordenadas por nombre, incluyendo la cantidad de
     * productos asociados a cada una (calculada en una sola consulta para evitar N+1).
     */
    @Transactional(readOnly = true)
    public List<CategoryResponseDTO> getCategories() {
        List<Category> categories = categoryRepository.findAll(Sort.by(Sort.Direction.ASC, "name"));
        Map<UUID, Integer> counts = productCountByCategory(categories);
        return categories.stream()
                .map(category -> mapToResponseDTO(category, counts.getOrDefault(category.getId(), 0)))
                .collect(Collectors.toList());
    }

    /**
     * Crea una nueva categoría. La unicidad del nombre la garantiza el índice único
     * de la tabla 'categories' (un nombre duplicado provoca un 409 Conflict).
     */
    @Transactional
    public CategoryResponseDTO createCategory(CategoryRequestDTO request) {
        Category category = new Category(UUID.randomUUID(), request.getName().trim(), request.getDescription());
        Category saved = categoryRepository.save(category);
        // Una categoría recién creada no tiene productos asociados.
        return mapToResponseDTO(saved, 0);
    }

    /**
     * Elimina una categoría. Rechaza el borrado si la categoría todavía tiene
     * productos asociados, para no dejar productos huérfanos.
     */
    @Transactional
    public void deleteCategory(@NonNull UUID id) {
        if (!categoryRepository.existsById(id)) {
            throw new EntityNotFoundException("La categoría no fue encontrada con el ID proporcionado: " + id);
        }
        long productCount = productRepository.countByCategory_Id(id);
        if (productCount > 0) {
            throw new CategoryInUseException(
                    "No se puede eliminar una categoría con productos asociados (" + productCount + ").");
        }
        categoryRepository.deleteById(id);
    }

    /**
     * Calcula el mapa categoryId -> cantidad de productos para el conjunto de categorías.
     * Las categorías sin productos quedan fuera del mapa (se resuelven como 0).
     */
    private Map<UUID, Integer> productCountByCategory(List<Category> categories) {
        List<UUID> ids = categories.stream()
                .map(Category::getId)
                .collect(Collectors.toList());
        if (ids.isEmpty()) {
            return Collections.emptyMap();
        }
        return productRepository.countProductsByCategoryIds(ids).stream()
                .collect(Collectors.toMap(CategoryProductCountView::getCategoryId,
                        view -> view.getTotal() != null ? view.getTotal().intValue() : 0));
    }

    private CategoryResponseDTO mapToResponseDTO(Category category, int productCount) {
        CategoryResponseDTO dto = new CategoryResponseDTO();
        dto.setId(category.getId());
        dto.setName(category.getName());
        dto.setDescription(category.getDescription());
        dto.setProductCount(productCount);
        return dto;
    }
}
