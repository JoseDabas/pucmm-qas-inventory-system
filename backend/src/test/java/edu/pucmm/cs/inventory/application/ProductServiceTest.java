package edu.pucmm.cs.inventory.application;

import edu.pucmm.cs.inventory.domain.Category;
import edu.pucmm.cs.inventory.infrastructure.persistence.entity.ProductEntity;
import edu.pucmm.cs.inventory.infrastructure.persistence.repository.CategoryJpaRepository;
import edu.pucmm.cs.inventory.infrastructure.persistence.repository.ProductJpaRepository;
import edu.pucmm.cs.inventory.infrastructure.persistence.repository.ProductStockView;
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
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para ProductService.
 * Verifica la correcta ejecución de la lógica de negocio, incluyendo la creación de productos,
 * actualizaciones, borrado lógico, manejo de alertas de stock crítico y validación de 
 * excepciones como EntityNotFoundException.
 */
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

    /**
     * Verifica la correcta creación de un producto en su escenario base (happy path).
     * Asegura que el servicio orqueste la delegación al repositorio y devuelva
     * el DTO correspondiente con los datos guardados.
     */
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

    /**
     * Valida que al crear un producto con un stock inicial especificado (> 0),
     * el sistema registre automáticamente un movimiento de entrada (IN) en el ledger de stock,
     * garantizando la trazabilidad desde el momento cero.
     */
    @Test
    @DisplayName("createProduct con cantidad inicial registra movimiento de stock")
    void createProductRegistraMovimiento() {
        when(categoryRepository.findByName(any())).thenReturn(Optional.empty());
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));
        when(productRepository.save(any(ProductEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        productService.createProduct(request);

        verify(stockMovementRepository, times(1)).save(any());
    }

    /**
     * Comprueba el manejo de productos sin categoría.
     * Si no se especifica una categoría en el request, el servicio no debe invocar
     * al CategoryJpaRepository, evitando consultas innecesarias a la base de datos.
     */
    @Test
    @DisplayName("createProduct sin categoria no consulta repositorio de categorias")
    void createProductSinCategoria() {
        request.setCategory(null);
        when(productRepository.save(any(ProductEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        productService.createProduct(request);

        verify(categoryRepository, never()).findByName(any());
    }

    /**
     * Asegura que el servicio respete la decisión del cliente respecto al estado activo/inactivo
     * de un nuevo producto, y no fuerce un estado 'true' si el usuario indicó explícitamente 'false'.
     */
    @Test
    @DisplayName("createProduct respeta isActive explicito cuando el cliente lo indica")
    void createProductRespetaIsActiveExplicito() {
        request.setIsActive(false);
        when(categoryRepository.findByName(any())).thenReturn(Optional.empty());
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));
        when(productRepository.save(any(ProductEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductResponseDTO result = productService.createProduct(request);

        assertFalse(result.getIsActive());
    }

    /**
     * Valida que intentar actualizar un producto inexistente
     * sea rechazado tempranamente lanzando jakarta.persistence.EntityNotFoundException,
     * protegiendo al sistema de actualizaciones huérfanas.
     */
    @Test
    @DisplayName("updateProduct con ID inexistente lanza excepcion")
    void updateProductInexistenteLanzaExcepcion() {
        UUID id = UUID.randomUUID();
        when(productRepository.findById(id)).thenReturn(Optional.empty());

        assertThrows(jakarta.persistence.EntityNotFoundException.class,
                () -> productService.updateProduct(id, request));
    }

    /**
     * Verifica que al actualizar un producto existente, los nuevos datos
     * sobrescriban a los anteriores y el producto persista correctamente.
     */
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

    /**
     * Comprueba que la eliminación (lógica) delegue correctamente
     * al repositorio siempre que el producto exista en la base de datos.
     */
    @Test
    @DisplayName("deleteProduct existente elimina")
    void deleteProductExistente() {
        UUID id = UUID.randomUUID();
        when(productRepository.existsById(id)).thenReturn(true);

        productService.deleteProduct(id);

        verify(stockMovementRepository, times(1)).deleteByProductId(id);
        verify(productRepository, times(1)).deleteById(id);
    }

    /**
     * Asegura que intentar eliminar un producto con un UUID no registrado
     * lance una excepción de negocio en lugar de fallar silenciosamente,
     * impidiendo llamadas en vano al repositorio.
     */
    @Test
    @DisplayName("deleteProduct inexistente lanza excepcion")
    void deleteProductInexistenteLanzaExcepcion() {
        UUID id = UUID.randomUUID();
        when(productRepository.existsById(id)).thenReturn(false);

        assertThrows(jakarta.persistence.EntityNotFoundException.class,
                () -> productService.deleteProduct(id));
        verify(productRepository, never()).deleteById(any());
    }

    /**
     * Verifica que si no se provee un término de búsqueda,
     * el servicio consulte la totalidad de productos (paginados), 
     * aplicando un orden descendente por fecha de creación por defecto.
     */
    @Test
    @DisplayName("getProducts sin busqueda consulta findByIsActiveTrue paginado")
    void getProductsSinBusquedaUsaFindAll() {
        Pageable pageable = PageRequest.of(0, 10);
        // Sin orden explícito, el servicio ordena por createdAt DESC (más reciente primero).
        Pageable expected = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        when(productRepository.findByIsActiveTrue(expected)).thenReturn(new PageImpl<>(List.of(new ProductEntity())));
        when(stockMovementRepository.sumSignedQuantitiesByProductIds(any())).thenReturn(List.of());

        Page<ProductResponseDTO> result = productService.getProducts(null, pageable);

        assertEquals(1, result.getTotalElements());
        verify(productRepository, times(1)).findByIsActiveTrue(expected);
        verify(productRepository, never())
                .findByNameContainingIgnoreCaseOrSkuCodeContainingIgnoreCase(any(), any(), any());
    }

    /**
     * Valida que si se provee una cadena de búsqueda,
     * el servicio ejecute la consulta filtrada combinando Nombre y SKU,
     * ignorando mayúsculas y minúsculas (case-insensitive).
     */
    @Test
    @DisplayName("getProducts con termino usa busqueda por nombre o SKU")
    void getProductsConBusquedaUsaFiltro() {
        Pageable pageable = PageRequest.of(0, 10);
        // Sin orden explícito, el servicio ordena por createdAt DESC (más reciente primero).
        Pageable expected = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        when(productRepository.findByNameContainingIgnoreCaseOrSkuCodeContainingIgnoreCase("lap", "lap", expected))
                .thenReturn(new PageImpl<>(List.of(new ProductEntity())));
        when(stockMovementRepository.sumSignedQuantitiesByProductIds(any())).thenReturn(List.of());

        Page<ProductResponseDTO> result = productService.getProducts("  lap  ", pageable);

        assertEquals(1, result.getTotalElements());
        verify(productRepository, times(1))
                .findByNameContainingIgnoreCaseOrSkuCodeContainingIgnoreCase("lap", "lap", expected);
        verify(productRepository, never()).findByIsActiveTrue(any(Pageable.class));
    }

    /**
     * Comprueba que la consulta paginada asigne a cada producto su stock actual
     * procesado de forma vectorizada desde la tabla de movimientos, 
     * evitando el antipatrón N+1 y entregando cifras en tiempo real.
     */
    @Test
    @DisplayName("getProducts calcula el stock actual desde el ledger de movimientos")
    void getProductsCalculaStockActualDesdeLedger() {
        UUID productId = UUID.randomUUID();
        ProductEntity product = new ProductEntity();
        product.setId(productId);
        product.setName("Laptop");
        Pageable pageable = PageRequest.of(0, 10);
        // Sin orden explícito, el servicio ordena por createdAt DESC (más reciente primero).
        Pageable expected = PageRequest.of(0, 10, Sort.by(Sort.Direction.DESC, "createdAt"));
        when(productRepository.findByIsActiveTrue(expected)).thenReturn(new PageImpl<>(List.of(product)));

        ProductStockView view = mock(ProductStockView.class);
        when(view.getProductId()).thenReturn(productId);
        when(view.getTotal()).thenReturn(35L);
        when(stockMovementRepository.sumSignedQuantitiesByProductIds(any())).thenReturn(List.of(view));

        Page<ProductResponseDTO> result = productService.getProducts(null, pageable);

        assertEquals(35, result.getContent().get(0).getStockActual());
    }

    /**
     * Asegura el comportamiento del patrón "Find-or-Create" de categorías:
     * si se asigna un producto a una categoría que ya existe, el servicio 
     * debe reutilizar la existente y no intentar duplicarla.
     */
    @Test
    @DisplayName("createProduct reutiliza categoria existente")
    void createProductReutilizaCategoria() {
        Category existing = new Category(UUID.randomUUID(), "Electronica", null);
        when(categoryRepository.findByName("Electronica")).thenReturn(Optional.of(existing));
        when(productRepository.save(any(ProductEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        productService.createProduct(request);

        verify(categoryRepository, never()).save(any(Category.class));
    }

    /**
     * Verifica que el endpoint de alertas críticas funcione correctamente,
     * obteniendo aquellos productos cuyo stock actual es menor o igual a su stock mínimo,
     * mapeándolos de manera consistente a DTOs.
     */
    @Test
    @DisplayName("getCriticalStockAlerts devuelve los productos con stock critico mapeados a DTO")
    void getCriticalStockAlertsDevuelveProductos() {
        ProductEntity critico = new ProductEntity();
        critico.setId(UUID.randomUUID());
        critico.setName("Producto critico");
        when(productRepository.findProductsWithCriticalStock()).thenReturn(List.of(critico));
        when(stockMovementRepository.sumSignedQuantitiesByProductIds(any())).thenReturn(List.of());

        List<ProductResponseDTO> result = productService.getCriticalStockAlerts();

        assertEquals(1, result.size());
        assertEquals("Producto critico", result.get(0).getName());
        verify(productRepository, times(1)).findProductsWithCriticalStock();
    }

    /**
     * Comprueba que al instanciar un producto nuevo y devolverlo inmediatamente
     * al cliente, la propiedad "stockActual" del DTO se refleje igual a la 
     * cantidad inicial ingresada, sin necesidad de consultar el ledger.
     */
    @Test
    @DisplayName("createProduct expone stockActual igual a la cantidad inicial")
    void createProductStockActualIgualaCantidadInicial() {
        when(categoryRepository.findByName(any())).thenReturn(Optional.empty());
        when(categoryRepository.save(any(Category.class))).thenAnswer(inv -> inv.getArgument(0));
        when(productRepository.save(any(ProductEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        ProductResponseDTO result = productService.createProduct(request);

        assertEquals(10, result.getStockActual());
    }

}
