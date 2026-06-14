package edu.pucmm.cs.inventory.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias de las validaciones de la entidad de dominio Product.
 */
public class ProductTest {

    // Helper para construir un producto válido base que cada test modifica.
    private Product validProduct() {
        return new Product(
                UUID.randomUUID(), "Laptop", "SKU-001", "Descripcion",
                null, new BigDecimal("100.00"), 10, 2, true);
    }

    @Test
    @DisplayName("Crea un producto valido sin lanzar excepcion")
    void creaProductoValido() {
        Product p = validProduct();
        assertEquals("Laptop", p.getName());
        assertEquals("SKU-001", p.getSkuCode());
        assertEquals(0, new BigDecimal("100.00").compareTo(p.getPrice()));
    }

    @Test
    @DisplayName("isActive nulo se normaliza a true por defecto")
    void isActiveNuloPorDefectoEsTrue() {
        Product p = new Product(
                UUID.randomUUID(), "Laptop", "SKU-001", null,
                null, new BigDecimal("100.00"), 10, 2, null);
        assertTrue(p.getIsActive());
    }

    // Validaciones que deben fallar

    @Test
    @DisplayName("ID nulo lanza excepcion")
    void idNuloLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> new Product(
                null, "Laptop", "SKU-001", null, null,
                new BigDecimal("100.00"), 10, 2, true));
    }

    @Test
    @DisplayName("Nombre nulo lanza excepcion")
    void nombreNuloLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> new Product(
                UUID.randomUUID(), null, "SKU-001", null, null,
                new BigDecimal("100.00"), 10, 2, true));
    }

    @Test
    @DisplayName("Nombre vacio lanza excepcion")
    void nombreVacioLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> new Product(
                UUID.randomUUID(), "   ", "SKU-001", null, null,
                new BigDecimal("100.00"), 10, 2, true));
    }

    @Test
    @DisplayName("SKU nulo lanza excepcion")
    void skuNuloLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> new Product(
                UUID.randomUUID(), "Laptop", null, null, null,
                new BigDecimal("100.00"), 10, 2, true));
    }

    @Test
    @DisplayName("SKU vacio lanza excepcion")
    void skuVacioLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> new Product(
                UUID.randomUUID(), "Laptop", "  ", null, null,
                new BigDecimal("100.00"), 10, 2, true));
    }

    @Test
    @DisplayName("Precio nulo lanza excepcion")
    void precioNuloLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> new Product(
                UUID.randomUUID(), "Laptop", "SKU-001", null, null,
                null, 10, 2, true));
    }

    @Test
    @DisplayName("Precio negativo lanza excepcion")
    void precioNegativoLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> new Product(
                UUID.randomUUID(), "Laptop", "SKU-001", null, null,
                new BigDecimal("-1.00"), 10, 2, true));
    }

    @Test
    @DisplayName("Cantidad inicial nula lanza excepcion")
    void cantidadInicialNulaLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> new Product(
                UUID.randomUUID(), "Laptop", "SKU-001", null, null,
                new BigDecimal("100.00"), null, 2, true));
    }

    @Test
    @DisplayName("Cantidad inicial negativa lanza excepcion")
    void cantidadInicialNegativaLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> new Product(
                UUID.randomUUID(), "Laptop", "SKU-001", null, null,
                new BigDecimal("100.00"), -5, 2, true));
    }

    @Test
    @DisplayName("Stock minimo nulo lanza excepcion")
    void stockMinimoNuloLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> new Product(
                UUID.randomUUID(), "Laptop", "SKU-001", null, null,
                new BigDecimal("100.00"), 10, null, true));
    }

    @Test
    @DisplayName("Stock minimo negativo lanza excepcion")
    void stockMinimoNegativoLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> new Product(
                UUID.randomUUID(), "Laptop", "SKU-001", null, null,
                new BigDecimal("100.00"), 10, -1, true));
    }

    // Setters con validacion

    @Test
    @DisplayName("setPrice con valor negativo lanza excepcion")
    void setPriceNegativoLanzaExcepcion() {
        Product p = validProduct();
        assertThrows(IllegalArgumentException.class, () -> p.setPrice(new BigDecimal("-10.00")));
    }

    @Test
    @DisplayName("setPrice con valor valido actualiza el precio")
    void setPriceValidoActualiza() {
        Product p = validProduct();
        p.setPrice(new BigDecimal("250.00"));
        assertEquals(0, new BigDecimal("250.00").compareTo(p.getPrice()));
    }

    @Test
    @DisplayName("setMinimumStock negativo lanza excepcion")
    void setMinimumStockNegativoLanzaExcepcion() {
        Product p = validProduct();
        assertThrows(IllegalArgumentException.class, () -> p.setMinimumStock(-3));
    }

    @Test
    @DisplayName("setIsActive nulo lanza excepcion")
    void setIsActiveNuloLanzaExcepcion() {
        Product p = validProduct();
        assertThrows(IllegalArgumentException.class, () -> p.setIsActive(null));
    }

    @Test
    @DisplayName("setName vacio lanza excepcion")
    void setNameVacioLanzaExcepcion() {
        Product p = validProduct();
        assertThrows(IllegalArgumentException.class, () -> p.setName(""));
    }

}
