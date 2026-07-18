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

@ExtendWith(MockitoExtension.class)
public class StockMovementServiceTest {

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

    @Test
    @DisplayName("getMovements sin busqueda usa findAll con orden por fecha descendente")
    void getMovementsSinBusquedaUsaFindAllOrdenadoPorFecha() {
        when(stockMovementRepository.findAll(any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(new StockMovementEntity())));
        when(productRepository.findAllById(any())).thenReturn(List.of());

        Page<StockMovementResponseDTO> result = stockMovementService.getMovements(null, PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        ArgumentCaptor<Pageable> captor = ArgumentCaptor.forClass(Pageable.class);
        verify(stockMovementRepository).findAll(captor.capture());
        Sort.Order order = captor.getValue().getSort().getOrderFor("date");
        assertNotNull(order);
        assertTrue(order.isDescending());
        verify(stockMovementRepository, never()).searchByProductNameOrUsername(any(), any());
    }

    @Test
    @DisplayName("getMovements con termino usa la busqueda por producto o usuario")
    void getMovementsConTerminoUsaBusqueda() {
        when(stockMovementRepository.searchByProductNameOrUsername(eq("lap"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(new StockMovementEntity())));
        when(productRepository.findAllById(any())).thenReturn(List.of());

        Page<StockMovementResponseDTO> result = stockMovementService.getMovements("  lap  ", PageRequest.of(0, 10));

        assertEquals(1, result.getTotalElements());
        verify(stockMovementRepository).searchByProductNameOrUsername(eq("lap"), any(Pageable.class));
        verify(stockMovementRepository, never()).findAll(any(Pageable.class));
    }

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
