package edu.pucmm.cs.inventory.integration;

import edu.pucmm.cs.inventory.application.StockMovementAuditService;
import edu.pucmm.cs.inventory.infrastructure.persistence.entity.ProductEntity;
import edu.pucmm.cs.inventory.infrastructure.persistence.entity.StockMovementEntity;
import edu.pucmm.cs.inventory.infrastructure.persistence.repository.ProductJpaRepository;
import edu.pucmm.cs.inventory.infrastructure.persistence.repository.StockMovementJpaRepository;
import edu.pucmm.cs.inventory.infrastructure.web.dto.StockMovementAuditResponseDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifica de punta a punta (Postgres real vía Testcontainers) que Hibernate Envers
 * registra las revisiones de un movimiento de stock y que el servicio de auditoría
 * las expone correctamente.
 */
class StockMovementAuditIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private StockMovementJpaRepository stockMovementRepository;

    @Autowired
    private ProductJpaRepository productRepository;

    @Autowired
    private StockMovementAuditService stockMovementAuditService;

    /**
     * Valida de principio a fin el flujo de auditoría de inventarios físicos.
     * Crea un movimiento inicial, lo modifica y afirma que Envers rastrea ambos
     * estados retornando el historial más reciente primero.
     */
    @Test
    @DisplayName("Envers registra revisiones ADD y MOD al crear y modificar un movimiento de stock")
    void auditHistoryRegistraAltaYModificacion() {
        ProductEntity p = new ProductEntity();
        p.setId(UUID.randomUUID());
        p.setName("Producto Base");
        p.setSkuCode("SKU-BASE-1");
        p.setPrice(new BigDecimal("100.00"));
        p.setInitialQuantity(10);
        p.setMinimumStock(2);
        p.setIsActive(true);
        ProductEntity product = productRepository.save(p);

        StockMovementEntity movement = new StockMovementEntity();
        movement.setId(UUID.randomUUID());
        movement.setProductId(product.getId());
        movement.setMovementType("IN");
        movement.setPreviousQuantity(10);
        movement.setNewQuantity(15);
        movement.setDate(LocalDateTime.now());
        movement.setUsername("user1");
        StockMovementEntity created = stockMovementRepository.save(movement);

        created.setObservations("Corregido por error de tipeo");
        stockMovementRepository.save(created);

        List<StockMovementAuditResponseDTO> history = stockMovementAuditService.getAllStockMovementAuditHistory();

        List<StockMovementAuditResponseDTO> movementHistory = history.stream()
                .filter(r -> r.getEntityId().equals(created.getId()))
                .toList();

        assertEquals(2, movementHistory.size());
        
        assertEquals("UPDATED", movementHistory.get(0).getRevisionType());
        assertEquals("Corregido por error de tipeo", movementHistory.get(0).getObservations());
        assertEquals(15, movementHistory.get(0).getNewQuantity());
        
        assertEquals("CREATED", movementHistory.get(1).getRevisionType());
        assertNull(movementHistory.get(1).getObservations());
        assertEquals("user1", movementHistory.get(1).getUsername());
        assertNotNull(movementHistory.get(0).getRevisionDate());
    }
}
