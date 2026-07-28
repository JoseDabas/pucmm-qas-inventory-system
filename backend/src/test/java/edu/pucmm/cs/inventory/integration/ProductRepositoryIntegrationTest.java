package edu.pucmm.cs.inventory.integration;

import edu.pucmm.cs.inventory.infrastructure.persistence.entity.ProductEntity;
import edu.pucmm.cs.inventory.infrastructure.persistence.repository.ProductJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de integración para {@link ProductJpaRepository}.
 * Verifican las operaciones CRUD reales contra un PostgreSQL efímero
 * levantado por Testcontainers, incluyendo restricciones de unicidad (SKU).
 */
class ProductRepositoryIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ProductJpaRepository productRepository;

    private ProductEntity buildProduct(String sku) {
        ProductEntity p = new ProductEntity();
        p.setId(UUID.randomUUID());
        p.setName("Producto Test");
        p.setSkuCode(sku);
        p.setPrice(new BigDecimal("100.00"));
        p.setInitialQuantity(10);
        p.setMinimumStock(2);
        p.setIsActive(true);
        return p;
    }

    // ---- Persistencia básica ----

    @Test
    @DisplayName("Guarda y recupera un producto correctamente en PostgreSQL real")
    void guardaYRecuperaProducto() {
        ProductEntity saved = productRepository.save(buildProduct("SKU-INT-1"));
        Optional<ProductEntity> found = productRepository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals("SKU-INT-1", found.get().getSkuCode());
    }

    // ---- Restricciones de unicidad ----

    @Test
    @DisplayName("SKU duplicado lanza excepción por constraint UNIQUE")
    void skuDuplicadoLanzaError() {
        productRepository.save(buildProduct("SKU-DUP"));
        ProductEntity duplicate = buildProduct("SKU-DUP");
        assertThrows(Exception.class, () -> productRepository.saveAndFlush(duplicate));
    }

    // ---- Eliminación ----

    // Test que verifica la eliminación (hard delete) de un producto.
    @Test
    @DisplayName("Elimina un producto y verifica que desaparece físicamente")
    void eliminaProducto() {
        ProductEntity saved = productRepository.save(buildProduct("SKU-DEL"));
        productRepository.deleteById(saved.getId());
        assertFalse(productRepository.findById(saved.getId()).isPresent());
    }

    // ---- Listado ----

    @Test
    @DisplayName("Lista todos los productos incluyendo los recién guardados")
    void listaTodosLosProductos() {
        productRepository.save(buildProduct("SKU-L1"));
        productRepository.save(buildProduct("SKU-L2"));
        assertTrue(productRepository.findAll().size() >= 2);
    }

    @Test
    @DisplayName("Producto no existente retorna Optional vacío")
    void productoNoExistenteRetornaVacio() {
        assertFalse(productRepository.findById(UUID.randomUUID()).isPresent());
    }
}
