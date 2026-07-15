package edu.pucmm.cs.inventory.application;

import edu.pucmm.cs.inventory.domain.Category;
import edu.pucmm.cs.inventory.infrastructure.persistence.entity.ProductEntity;
import edu.pucmm.cs.inventory.infrastructure.persistence.repository.CategoryJpaRepository;
import edu.pucmm.cs.inventory.infrastructure.persistence.repository.ProductJpaRepository;
import edu.pucmm.cs.inventory.infrastructure.persistence.repository.StockMovementJpaRepository;
import edu.pucmm.cs.inventory.infrastructure.web.dto.ProductRequestDTO;
import edu.pucmm.cs.inventory.infrastructure.web.dto.ProductResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ProductServiceTest {

    @Mock
    private ProductJpaRepository productRepository;
    @Mock
    private StockMovementJpaRepository stockMovementRepository;
    @Mock
    private CategoryJpaRepository categoryRepository;

    @InjectMocks
    private ProductService productService;

    private ProductRequestDTO request;

    @BeforeEach
    void setUp() {
        request = new ProductRequestDTO();
        request.setName("Laptop");
        request.setSkuCode("SKU-001");
        request.setDescription("Una laptop");
        request.setCategory("Electronica");
        request.setPrice(new BigDecimal("100.00"));
        request.setInitialQuantity(10);
        request.setMinimumStock(2);
    }

    @Test
    @DisplayName("createProduct guarda el producto y devuelve DTO")
    void createProductGuardaYDevuelve() {
        when(categoryRepository.findByName("Electronica")).thenReturn(Optional.empty());
        when(categoryRepository.save(any(Category.class)))
                .thenAnswer(inv -> inv.getArgument(0));
        when(productRepository.save(any(ProductEntity.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        ProductResponseDTO result = productService.createProduct(request);

        assertNotNull(result);
        assertEquals("Laptop", result.getName());
        verify(productRepository, times(1)).save(any(ProductEntity.class));
    }

    @Test
    @DisplayName("createProduct con cantidad inicial registra movimiento de stock")
    void createProductRegistraMovimiento() {
        when(categoryRepository.findByName(any())).thenReturn(Optional.empty());
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));
        when(productRepository.save(any(ProductEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        productService.createProduct(request);

        verify(stockMovementRepository, times(1)).save(any());
    }

    @Test
    @DisplayName("createProduct sin categoria no consulta repositorio de categorias")
    void createProductSinCategoria() {
        request.setCategory(null);
        when(productRepository.save(any(ProductEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        productService.createProduct(request);

        verify(categoryRepository, never()).findByName(any());
    }

    @Test
    @DisplayName("updateProduct con ID inexistente lanza excepcion")
    void updateProductInexistenteLanzaExcepcion() {
        UUID id = UUID.randomUUID();
        when(productRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(jakarta.persistence.EntityNotFoundException.class,
                () -> productService.updateProduct(id, request));
    }

    @Test
    @DisplayName("updateProduct existente actualiza y devuelve DTO")
    void updateProductExistenteActualiza() {
        UUID id = UUID.randomUUID();
        ProductEntity existing = new ProductEntity();
        existing.setId(id);
        existing.setName("Viejo");
        when(productRepository.findById(id)).thenReturn(Optional.of(existing));
        when(categoryRepository.findByName(any())).thenReturn(Optional.empty());
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));
        when(productRepository.save(any(ProductEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductResponseDTO result = productService.updateProduct(id, request);

        assertEquals("Laptop", result.getName());
    }

    @Test
    @DisplayName("deleteProduct existente elimina")
    void deleteProductExistente() {
        UUID id = UUID.randomUUID();
        when(productRepository.existsById(id)).thenReturn(true);

        productService.deleteProduct(id);

        verify(productRepository, times(1)).deleteById(id);
    }

    @Test
    @DisplayName("deleteProduct inexistente lanza excepcion")
    void deleteProductInexistenteLanzaExcepcion() {
        UUID id = UUID.randomUUID();
        when(productRepository.existsById(id)).thenReturn(false);

        assertThrows(jakarta.persistence.EntityNotFoundException.class,
                () -> productService.deleteProduct(id));
        verify(productRepository, never()).deleteById(any());
    }

    @Test
    @DisplayName("getProducts sin busqueda consulta findAll paginado")
    void getProductsSinBusquedaUsaFindAll() {
        Pageable pageable = PageRequest.of(0, 10);
        when(productRepository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(new ProductEntity())));

        Page<ProductResponseDTO> result = productService.getProducts(null, pageable);

        assertEquals(1, result.getTotalElements());
        verify(productRepository, times(1)).findAll(pageable);
        verify(productRepository, never())
                .findByNameContainingIgnoreCaseOrSkuCodeContainingIgnoreCase(any(), any(), any());
    }

    @Test
    @DisplayName("getProducts con termino usa busqueda por nombre o SKU")
    void getProductsConBusquedaUsaFiltro() {
        Pageable pageable = PageRequest.of(0, 10);
        when(productRepository.findByNameContainingIgnoreCaseOrSkuCodeContainingIgnoreCase("lap", "lap", pageable))
                .thenReturn(new PageImpl<>(List.of(new ProductEntity())));

        Page<ProductResponseDTO> result = productService.getProducts("  lap  ", pageable);

        assertEquals(1, result.getTotalElements());
        verify(productRepository, times(1))
                .findByNameContainingIgnoreCaseOrSkuCodeContainingIgnoreCase("lap", "lap", pageable);
        verify(productRepository, never()).findAll(any(Pageable.class));
    }

    @Test
    @DisplayName("createProduct reutiliza categoria existente")
    void createProductReutilizaCategoria() {
        Category existing = new Category(UUID.randomUUID(), "Electronica", null);
        when(categoryRepository.findByName("Electronica")).thenReturn(Optional.of(existing));
        when(productRepository.save(any(ProductEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        productService.createProduct(request);

        verify(categoryRepository, never()).save(any(Category.class));
    }

}
