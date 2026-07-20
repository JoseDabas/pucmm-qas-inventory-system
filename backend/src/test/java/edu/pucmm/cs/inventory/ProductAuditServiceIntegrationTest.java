package edu.pucmm.cs.inventory;

import edu.pucmm.cs.inventory.application.ProductAuditService;
import edu.pucmm.cs.inventory.infrastructure.persistence.entity.ProductEntity;
import edu.pucmm.cs.inventory.infrastructure.persistence.repository.ProductJpaRepository;
import edu.pucmm.cs.inventory.infrastructure.web.dto.ProductAuditResponseDTO;
import jakarta.persistence.EntityNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifica de punta a punta (Postgres real vía Testcontainers) que Hibernate Envers
 * registra las revisiones de un producto y que el servicio de auditoría las expone
 * correctamente. Cada save del repositorio confirma su transacción, generando la
 * revisión de Envers.
 */
class ProductAuditServiceIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ProductJpaRepository productRepository;

    @Autowired
    private ProductAuditService productAuditService;

    private ProductEntity buildProduct(String sku) {
        ProductEntity p = new ProductEntity();
        p.setId(UUID.randomUUID());
        p.setName("Producto Auditado");
        p.setSkuCode(sku);
        p.setPrice(new BigDecimal("100.00"));
        p.setInitialQuantity(10);
        p.setMinimumStock(2);
        p.setIsActive(true);
        return p;
    }

    @Test
    void auditHistoryRegistraAltaYModificacion() {
        ProductEntity created = productRepository.save(buildProduct("SKU-AUD-1")); // revisión ADD

        created.setMinimumStock(5);
        created.setIsActive(false);
        productRepository.save(created); // revisión MOD

        List<ProductAuditResponseDTO> history = productAuditService.getProductAuditHistory(created.getId());

        assertEquals(2, history.size());
        // Orden descendente: la revisión más reciente (modificación) primero.
        assertEquals("UPDATED", history.get(0).getRevisionType());
        assertEquals(5, history.get(0).getMinimumStock());
        assertFalse(history.get(0).getIsActive());
        assertEquals("CREATED", history.get(1).getRevisionType());
        assertEquals(2, history.get(1).getMinimumStock());
        assertNotNull(history.get(0).getRevisionDate());
    }

    @Test
    void productoSinHistorialLanzaExcepcion() {
        assertThrows(EntityNotFoundException.class,
                () -> productAuditService.getProductAuditHistory(UUID.randomUUID()));
    }
}
