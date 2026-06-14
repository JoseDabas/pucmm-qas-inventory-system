package edu.pucmm.cs.inventory;

import edu.pucmm.cs.inventory.infrastructure.persistence.entity.ProductEntity;
import edu.pucmm.cs.inventory.infrastructure.persistence.repository.ProductJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ProductRepositoryIntegrationTest extends AbstractIntegrationTest {

    // Inyectamos el repositorio JPA para interactuar con la base de datos real del
    // contenedor.
    // Autowired hace que Spring Boot cree una instancia real del repositorio,
    // conectada al contenedor PostgreSQL levantado por Testcontainers.
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

    // @Test hace que estos métodos sean ejecutados como tests de integración,
    // verificando la interacción real con la base de datos.

    // Test de integración que verifica la persistencia y recuperación de un
    // producto.

    @Test
    void guardaYRecuperaProducto() {
        ProductEntity saved = productRepository.save(buildProduct("SKU-INT-1"));
        Optional<ProductEntity> found = productRepository.findById(saved.getId());
        assertTrue(found.isPresent());
        assertEquals("SKU-INT-1", found.get().getSkuCode());
    }

    // Verifica que el SKU es único y que intentar guardar un producto con un SKU
    @Test
    void skuDuplicadoLanzaError() {
        productRepository.save(buildProduct("SKU-DUP"));
        ProductEntity duplicate = buildProduct("SKU-DUP");
        assertThrows(Exception.class, () -> productRepository.saveAndFlush(duplicate));
    }

    // Test que verifica la eliminación de un producto.
    @Test
    void eliminaProducto() {
        ProductEntity saved = productRepository.save(buildProduct("SKU-DEL"));
        productRepository.deleteById(saved.getId());
        assertFalse(productRepository.findById(saved.getId()).isPresent());
    }

    // Verifica que se pueden listar todos los productos y que al menos los que
    // acabamos de guardar están presentes.
    @Test
    void listaTodosLosProductos() {
        productRepository.save(buildProduct("SKU-L1"));
        productRepository.save(buildProduct("SKU-L2"));
        assertTrue(productRepository.findAll().size() >= 2);
    }

    // Verifica que buscar un producto por un ID inexistente retorna un Optional
    // vacío.
    @Test
    void productoNoExistenteRetornaVacio() {
        assertFalse(productRepository.findById(UUID.randomUUID()).isPresent());
    }
}