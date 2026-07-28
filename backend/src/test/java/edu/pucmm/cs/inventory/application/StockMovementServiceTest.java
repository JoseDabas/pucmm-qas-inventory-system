package edu.pucmm.cs.inventory.application;

import edu.pucmm.cs.inventory.domain.MovementType;
import edu.pucmm.cs.inventory.infrastructure.persistence.entity.ProductEntity;
import edu.pucmm.cs.inventory.infrastructure.persistence.entity.StockMovementEntity;
import edu.pucmm.cs.inventory.infrastructure.persistence.repository.ProductJpaRepository;
import edu.pucmm.cs.inventory.infrastructure.persistence.repository.StockMovementJpaRepository;
import edu.pucmm.cs.inventory.infrastructure.web.dto.StockMovementRequestDTO;
import edu.pucmm.cs.inventory.infrastructure.web.dto.StockMovementResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias para StockMovementService.
 * Verifica las reglas de negocio en el registro de entradas y salidas de inventario,
 * asegurando la integridad del stock y el manejo de condiciones límite como sobregiros.
 */
@ExtendWith(MockitoExtension.class)
class StockMovementServiceTest {

    @Mock
    private StockMovementJpaRepository stockMovementRepository;
    @Mock
    private ProductJpaRepository productRepository;

    @InjectMocks
    private StockMovementService stockMovementService;

    private ProductEntity buildProduct(UUID id, String name) {
        ProductEntity product = new ProductEntity();
        product.setId(id);
        product.setName(name);
        return product;
    }

    private StockMovementRequestDTO buildRequest(UUID productId, MovementType type, int quantity) {
        StockMovementRequestDTO request = new StockMovementRequestDTO();
        request.setProductId(productId);
        request.setMovementType(type);
        request.setQuantity(quantity);
        request.setObservations("test");
        return request;
    }

    /**
     * Asegura que el servicio devuelva el historial completo ordenado 
     * por fecha de forma descendente (del más reciente al más antiguo) 
     * cuando no se aplican filtros de búsqueda.
     */
    @Test
    @DisplayName("getMovements sin busqueda usa findByProductIsActiveTrue con orden por fecha descendente")
    void getMovementsSinBusquedaUsaFindAllOrdenadoPorFecha() {
        when(stockMovementRepository.findByProductIsActiveTrue(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(new StockMovementEntity())));
        when(productRepository.findAllById(any())).thenReturn(List.of());

        Page<StockMovementResponseDTO> result = stockMovementService.getMovements(null, PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(stockMovementRepository).findByProductIsActiveTrue(captor.capture());
        Sort.Order order = captor.getValue().getSort().getOrderFor("date");
        assertNotNull(order);
        assertTrue(order.isDescending());
        verify(stockMovementRepository, never()).searchByProductNameOrUsername(any(), any());
    }

    /**
     * Verifica que si se envía un término de búsqueda, el servicio cambie la estrategia
     * de consulta y utilice el filtro por nombre de producto o usuario, optimizando 
     * la localización de movimientos específicos.
     */
    @Test
    @DisplayName("getMovements con termino usa la busqueda por producto o usuario")
    void getMovementsConTerminoUsaBusqueda() {
        when(stockMovementRepository.searchByProductNameOrUsername(eq("lap"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(new StockMovementEntity())));
        when(productRepository.findAllById(any())).thenReturn(List.of());

        Page<StockMovementResponseDTO> result = stockMovementService.getMovements("  lap  ", PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        verify(stockMovementRepository).searchByProductNameOrUsername(eq("lap"), any(Pageable.class));
        verify(stockMovementRepository, never()).findByProductIsActiveTrue(any(Pageable.class));
    }

    /**
     * Comprueba la lógica fundamental de una entrada de mercancía (IN).
     * El servicio debe calcular el balance anterior desde el ledger y 
     * registrar la nueva cantidad sumando estrictamente el valor ingresado.
     */
    @Test
    @DisplayName("registerMovement IN calcula cantidad anterior y nueva sumando al stock")
    void registerMovementInSuma() {
        UUID productId = UUID.randomUUID();
        when(productRepository.findById(productId)).thenReturn(Optional.of(buildProduct(productId, "Laptop")));
        when(stockMovementRepository.sumSignedQuantityByProductId(productId)).thenReturn(40);
        when(stockMovementRepository.save(any(StockMovementEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        StockMovementResponseDTO result = stockMovementService.registerMovement(
                buildRequest(productId, MovementType.IN, 10));

        assertEquals(40, result.getPreviousQuantity());
        assertEquals(50, result.getNewQuantity());
        assertEquals("IN", result.getMovementType());
        assertEquals("Laptop", result.getProductName());
    }

    /**
     * Comprueba la lógica fundamental de una salida de mercancía (OUT).
     * El servicio debe asegurar que se registre un decremento exacto en el stock 
     * basado en la cantidad solicitada.
     */
    @Test
    @DisplayName("registerMovement OUT valido resta del stock")
    void registerMovementOutResta() {
        UUID productId = UUID.randomUUID();
        when(productRepository.findById(productId)).thenReturn(Optional.of(buildProduct(productId, "Laptop")));
        when(stockMovementRepository.sumSignedQuantityByProductId(productId)).thenReturn(40);
        when(stockMovementRepository.save(any(StockMovementEntity.class))).thenAnswer(inv -> inv.getArgument(0));

        StockMovementResponseDTO result = stockMovementService.registerMovement(
                buildRequest(productId, MovementType.OUT, 10));

        assertEquals(40, result.getPreviousQuantity());
        assertEquals(30, result.getNewQuantity());
    }

    /**
     * Valida una regla de negocio crítica: el sistema no debe permitir que el inventario
     * quede en negativo. Si una salida (OUT) supera el stock actual disponible, 
     * debe lanzar IllegalArgumentException y abortar la transacción.
     */
    @Test
    @DisplayName("registerMovement OUT que supera el stock lanza excepcion y no guarda")
    void registerMovementOutInsuficienteLanzaExcepcion() {
        UUID productId = UUID.randomUUID();
        when(productRepository.findById(productId)).thenReturn(Optional.of(buildProduct(productId, "Laptop")));
        when(stockMovementRepository.sumSignedQuantityByProductId(productId)).thenReturn(40);

        assertThrows(IllegalArgumentException.class,
                () -> stockMovementService.registerMovement(buildRequest(productId, MovementType.OUT, 100)));
        verify(stockMovementRepository, never()).save(any());
    }

    /**
     * Protege contra anomalías o inyecciones de datos, asegurando que no se pueda 
     * registrar un movimiento hacia un producto que no existe físicamente en 
     * la base de datos (lanza EntityNotFoundException).
     */
    @Test
    @DisplayName("registerMovement con producto inexistente lanza EntityNotFoundException")
    void registerMovementProductoInexistenteLanzaExcepcion() {
        UUID productId = UUID.randomUUID();
        when(productRepository.findById(productId)).thenReturn(Optional.empty());

        assertThrows(jakarta.persistence.EntityNotFoundException.class,
                () -> stockMovementService.registerMovement(buildRequest(productId, MovementType.IN, 10)));
        verify(stockMovementRepository, never()).save(any());
    }
}
