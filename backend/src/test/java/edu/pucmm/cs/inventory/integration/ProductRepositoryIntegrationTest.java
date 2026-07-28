package edu.pucmm.cs.inventory.integration;

import edu.pucmm.cs.inventory.infrastructure.persistence.entity.ProductEntity;
import edu.pucmm.cs.inventory.infrastructure.persistence.repository.ProductJpaRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de integración para ProductJpaRepository.
 * Verifican las operaciones CRUD reales contra un PostgreSQL efímero
 * levantado por Testcontainers, incluyendo restricciones de unicidad (SKU).
 */
class ProductRepositoryIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private ProductJpaRepository productRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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

    /**
     * Valida que la persistencia mapee correctamente la entidad 
     * y pueda ser recuperada con los mismos datos insertados.
     */
    @Test
    @DisplayName("Guarda y recupera un producto correctamente en PostgreSQL real")
    void guardaYRecuperaProducto() {
        ProductEntity saved = productRepository.save(buildProduct("SKU-INT-1"));
        Optional<ProductEntity> found = productRepository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals("SKU-INT-1", found.get().getSkuCode());
    }

    /**
     * Comprueba que la base de datos haga cumplir la restricción UNIQUE 
     * sobre el SKU, lanzando excepción en caso de colisión.
     */
    @Test
    @DisplayName("SKU duplicado lanza excepción por constraint UNIQUE")
    void skuDuplicadoLanzaError() {
        productRepository.save(buildProduct("SKU-DUP"));
        ProductEntity duplicate = buildProduct("SKU-DUP");
        assertThrows(Exception.class, () -> productRepository.saveAndFlush(duplicate));
    }

    /**
     * Verifica que la eliminación es un borrado lógico (soft delete): el producto
     * deja de ser visible por las consultas de Hibernate, pero la fila 
     * sigue existiendo en la base de datos marcada con deleted = true.
     */
    @Test
    @DisplayName("Elimina un producto y verifica que es un Soft Delete (no desaparece físicamente)")
    void eliminaProductoEsSoftDelete() {
        ProductEntity saved = productRepository.save(buildProduct("SKU-DEL"));
        UUID id = saved.getId();

        productRepository.deleteById(id);

        assertFalse(productRepository.findById(id).isPresent());

        Integer vivos = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM products WHERE id = ? AND deleted = true", Integer.class, id);
        assertEquals(1, vivos);
    }

    /**
     * Asegura que el listado global consolide correctamente los 
     * registros recién inyectados en la sesión.
     */
    @Test
    @DisplayName("Lista todos los productos incluyendo los recién guardados")
    void listaTodosLosProductos() {
        productRepository.save(buildProduct("SKU-L1"));
        productRepository.save(buildProduct("SKU-L2"));
        assertTrue(productRepository.findAll().size() >= 2);
    }

    /**
     * Valida la ausencia de falsos positivos cuando se solicita 
     * un identificador inexistente.
     */
    @Test
    @DisplayName("Producto no existente retorna Optional vacío")
    void productoNoExistenteRetornaVacio() {
        assertFalse(productRepository.findById(UUID.randomUUID()).isPresent());
    }
}
