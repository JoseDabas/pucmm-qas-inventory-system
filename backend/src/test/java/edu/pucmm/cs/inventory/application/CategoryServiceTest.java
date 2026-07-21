package edu.pucmm.cs.inventory.application;

import edu.pucmm.cs.inventory.domain.Category;
import edu.pucmm.cs.inventory.infrastructure.persistence.repository.CategoryJpaRepository;
import edu.pucmm.cs.inventory.infrastructure.persistence.repository.CategoryProductCountView;
import edu.pucmm.cs.inventory.infrastructure.persistence.repository.ProductJpaRepository;
import edu.pucmm.cs.inventory.infrastructure.web.dto.CategoryRequestDTO;
import edu.pucmm.cs.inventory.infrastructure.web.dto.CategoryResponseDTO;
import edu.pucmm.cs.inventory.infrastructure.web.exception.CategoryInUseException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class CategoryServiceTest {

    @Mock
    private CategoryJpaRepository categoryRepository;
    @Mock
    private ProductJpaRepository productRepository;

    @InjectMocks
    private CategoryService categoryService;

    private CategoryRequestDTO buildRequest(String name) {
        CategoryRequestDTO request = new CategoryRequestDTO();
        request.setName(name);
        request.setDescription("desc");
        return request;
    }

    @Test
    @DisplayName("createCategory guarda y devuelve DTO con 0 productos")
    void createCategoryGuardaYDevuelve() {
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));

        CategoryResponseDTO result = categoryService.createCategory(buildRequest("Electrónica"));

        assertNotNull(result);
        assertEquals("Electrónica", result.getName());
        assertEquals(0, result.getProductCount());
        verify(categoryRepository, times(1)).save(any(Category.class));
    }

    @Test
    @DisplayName("deleteCategory con productos asociados lanza CategoryInUseException y no borra")
    void deleteCategoryConProductosLanzaExcepcion() {
        UUID id = UUID.randomUUID();
        when(categoryRepository.existsById(id)).thenReturn(true);
        when(productRepository.countByCategory_Id(id)).thenReturn(3L);

        assertThrows(CategoryInUseException.class, () -> categoryService.deleteCategory(id));
        verify(categoryRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("deleteCategory sin productos elimina")
    void deleteCategorySinProductosElimina() {
        UUID id = UUID.randomUUID();
        when(categoryRepository.existsById(id)).thenReturn(true);
        when(productRepository.countByCategory_Id(id)).thenReturn(0L);

        categoryService.deleteCategory(id);

        verify(categoryRepository, times(1)).deleteById(id);
    }

    @Test
    @DisplayName("deleteCategory inexistente lanza EntityNotFoundException")
    void deleteCategoryInexistenteLanzaExcepcion() {
        UUID id = UUID.randomUUID();
        when(categoryRepository.existsById(id)).thenReturn(false);

        assertThrows(jakarta.persistence.EntityNotFoundException.class,
                () -> categoryService.deleteCategory(id));
        verify(categoryRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("getCategories mapea la cantidad de productos desde el conteo por lote")
    void getCategoriesMapeaConteo() {
        UUID categoryId = UUID.randomUUID();
        Category category = new Category(categoryId, "Electrónica", null);
        when(categoryRepository.findAll(any(org.springframework.data.domain.Sort.class)))
                .thenReturn(List.of(category));

        CategoryProductCountView view = mock(CategoryProductCountView.class);
        when(view.getCategoryId()).thenReturn(categoryId);
        when(view.getTotal()).thenReturn(7L);
        when(productRepository.countProductsByCategoryIds(any())).thenReturn(List.of(view));

        List<CategoryResponseDTO> result = categoryService.getCategories();

        assertEquals(1, result.size());
        assertEquals(7, result.get(0).getProductCount());
        assertEquals("Electrónica", result.get(0).getName());
    }

    @Test
    @DisplayName("getCategories devuelve 0 productos para categorías sin movimientos de conteo")
    void getCategoriesSinConteoDevuelveCero() {
        Category category = new Category(UUID.randomUUID(), "Vacía", null);
        when(categoryRepository.findAll(any(org.springframework.data.domain.Sort.class)))
                .thenReturn(List.of(category));
        when(productRepository.countProductsByCategoryIds(any())).thenReturn(List.of());

        List<CategoryResponseDTO> result = categoryService.getCategories();

        assertEquals(0, result.get(0).getProductCount());
    }
}
