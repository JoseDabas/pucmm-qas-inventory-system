package edu.pucmm.cs.inventory.integration;

import edu.pucmm.cs.inventory.application.ProductAuditService;
import edu.pucmm.cs.inventory.infrastructure.persistence.entity.ProductEntity;
import edu.pucmm.cs.inventory.infrastructure.persistence.repository.ProductJpaRepository;
import edu.pucmm.cs.inventory.infrastructure.web.dto.ProductAuditResponseDTO;
import org.junit.jupiter.api.DisplayName;
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
    @DisplayName("Envers registra revisiones ADD y MOD al crear y modificar un producto")
    void auditHistoryRegistraAltaYModificacion() {
        ProductEntity created = productRepository.save(buildProduct("SKU-AUD-1")); // revisión ADD

        created.setMinimumStock(5);
        created.setIsActive(false);
        productRepository.save(created); // revisión MOD

        List<ProductAuditResponseDTO> history = productAuditService.getAllProductAuditHistory();

        // Filtramos para asegurar que encontramos las revisiones de nuestro producto (ya que es global)
        List<ProductAuditResponseDTO> productHistory = history.stream()
                .filter(r -> r.getEntityId().equals(created.getId()))
                .toList();

        assertEquals(2, productHistory.size());
        // Orden descendente: la revisión más reciente (modificación) primero.
        assertEquals("UPDATED", productHistory.get(0).getRevisionType());
        assertEquals(5, productHistory.get(0).getMinimumStock());
        assertFalse(productHistory.get(0).getIsActive());
        assertEquals("CREATED", productHistory.get(1).getRevisionType());
        assertEquals(2, productHistory.get(1).getMinimumStock());
        assertNotNull(productHistory.get(0).getRevisionDate());
    }
}
