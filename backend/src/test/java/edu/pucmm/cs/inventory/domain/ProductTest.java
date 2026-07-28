package edu.pucmm.cs.inventory.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias de las validaciones de la entidad de dominio Product.
 * Garantiza que las restricciones críticas de consistencia de inventario,
 * como los límites de stock y valores financieros correctos, se apliquen 
 * directamente en la memoria antes de llegar a la base de datos.
 */
public class ProductTest {

    // Helper para construir un producto válido base que cada test modifica.
    private Product validProduct() {
        return new Product(
                UUID.randomUUID(), "Laptop", "SKU-001", "Descripcion",
                null, new BigDecimal("100.00"), 10, 2, true);
    }

    /**
     * Comprueba la instanciación de un producto cuando todos los datos
     * suministrados son válidos y cumplen las políticas de la empresa.
     */
    @Test
    @DisplayName("Crea un producto valido sin lanzar excepcion")
    void creaProductoValido() {
        Product p = validProduct();
        assertEquals("Laptop", p.getName());
        assertEquals("SKU-001", p.getSkuCode());
        assertEquals(0, new BigDecimal("100.00").compareTo(p.getPrice()));
    }

    /**
     * Verifica que si no se define explícitamente el estado de actividad del producto,
     * este se inicie como activo (true) de manera predeterminada.
     */
    @Test
    @DisplayName("isActive nulo se normaliza a true por defecto")
    void isActiveNuloPorDefectoEsTrue() {
        Product p = new Product(
                UUID.randomUUID(), "Laptop", "SKU-001", null,
                null, new BigDecimal("100.00"), 10, 2, null);
        assertTrue(p.getIsActive());
    }

    // Validaciones que deben fallar

    /**
     * Asegura la presencia obligatoria del identificador único del producto.
     */
    @Test
    @DisplayName("ID nulo lanza excepcion")
    void idNuloLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> new Product(
                null, "Laptop", "SKU-001", null, null,
                new BigDecimal("100.00"), 10, 2, true));
    }

    /**
     * Valida que no se puedan inicializar productos con nombre nulo.
     */
    @Test
    @DisplayName("Nombre nulo lanza excepcion")
    void nombreNuloLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> new Product(
                UUID.randomUUID(), null, "SKU-001", null, null,
                new BigDecimal("100.00"), 10, 2, true));
    }

    /**
     * Valida que no se puedan inicializar productos con nombre en blanco,
     * garantizando su correcta presentación visual y búsqueda.
     */
    @Test
    @DisplayName("Nombre vacio lanza excepcion")
    void nombreVacioLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> new Product(
                UUID.randomUUID(), "   ", "SKU-001", null, null,
                new BigDecimal("100.00"), 10, 2, true));
    }

    /**
     * Asegura la presencia obligatoria del código SKU para la gestión de almacén.
     */
    @Test
    @DisplayName("SKU nulo lanza excepcion")
    void skuNuloLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> new Product(
                UUID.randomUUID(), "Laptop", null, null, null,
                new BigDecimal("100.00"), 10, 2, true));
    }

    /**
     * Comprueba que el código SKU contenga caracteres válidos y no solo espacios.
     */
    @Test
    @DisplayName("SKU vacio lanza excepcion")
    void skuVacioLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> new Product(
                UUID.randomUUID(), "Laptop", "  ", null, null,
                new BigDecimal("100.00"), 10, 2, true));
    }

    /**
     * Exige que el precio base esté explícitamente definido para fines contables.
     */
    @Test
    @DisplayName("Precio nulo lanza excepcion")
    void precioNuloLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> new Product(
                UUID.randomUUID(), "Laptop", "SKU-001", null, null,
                null, 10, 2, true));
    }

    /**
     * Protege el sistema contra alteraciones financieras, 
     * prohibiendo la asignación de precios por debajo de cero.
     */
    @Test
    @DisplayName("Precio negativo lanza excepcion")
    void precioNegativoLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> new Product(
                UUID.randomUUID(), "Laptop", "SKU-001", null, null,
                new BigDecimal("-1.00"), 10, 2, true));
    }

    /**
     * Garantiza que la cantidad inicial sea declarada (puede ser cero, pero no nula)
     * para inicializar correctamente el registro contable de stock.
     */
    @Test
    @DisplayName("Cantidad inicial nula lanza excepcion")
    void cantidadInicialNulaLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> new Product(
                UUID.randomUUID(), "Laptop", "SKU-001", null, null,
                new BigDecimal("100.00"), null, 2, true));
    }

    /**
     * Verifica que no se pueda declarar un inventario inicial con deuda física
     * o números negativos.
     */
    @Test
    @DisplayName("Cantidad inicial negativa lanza excepcion")
    void cantidadInicialNegativaLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> new Product(
                UUID.randomUUID(), "Laptop", "SKU-001", null, null,
                new BigDecimal("100.00"), -5, 2, true));
    }

    /**
     * Obliga a declarar un nivel de alerta de stock mínimo, esencial 
     * para los motores de notificación.
     */
    @Test
    @DisplayName("Stock minimo nulo lanza excepcion")
    void stockMinimoNuloLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> new Product(
                UUID.randomUUID(), "Laptop", "SKU-001", null, null,
                new BigDecimal("100.00"), 10, null, true));
    }

    /**
     * Impide configurar la alerta de stock por debajo de cero, 
     * previniendo comportamientos indefinidos en las métricas.
     */
    @Test
    @DisplayName("Stock minimo negativo lanza excepcion")
    void stockMinimoNegativoLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class, () -> new Product(
                UUID.randomUUID(), "Laptop", "SKU-001", null, null,
                new BigDecimal("100.00"), 10, -1, true));
    }

    // Setters con validacion

    /**
     * Comprueba que la validación de precio no aplique únicamente en creación,
     * sino también en la re-asignación de costos durante el ciclo de vida del producto.
     */
    @Test
    @DisplayName("setPrice con valor negativo lanza excepcion")
    void setPriceNegativoLanzaExcepcion() {
        Product p = validProduct();
        assertThrows(IllegalArgumentException.class, () -> p.setPrice(new BigDecimal("-10.00")));
    }

    /**
     * Verifica la actualización exitosa de precios y su exactitud decimal.
     */
    @Test
    @DisplayName("setPrice con valor valido actualiza el precio")
    void setPriceValidoActualiza() {
        Product p = validProduct();
        p.setPrice(new BigDecimal("250.00"));
        assertEquals(0, new BigDecimal("250.00").compareTo(p.getPrice()));
    }

    /**
     * Asegura que el setter para stock mínimo implemente las restricciones
     * matemáticas del dominio, protegiendo actualizaciones defectuosas de usuario.
     */
    @Test
    @DisplayName("setMinimumStock negativo lanza excepcion")
    void setMinimumStockNegativoLanzaExcepcion() {
        Product p = validProduct();
        assertThrows(IllegalArgumentException.class, () -> p.setMinimumStock(-3));
    }

    /**
     * Verifica que no se pueda dejar en estado desconocido el atributo de visibilidad.
     */
    @Test
    @DisplayName("setIsActive nulo lanza excepcion")
    void setIsActiveNuloLanzaExcepcion() {
        Product p = validProduct();
        assertThrows(IllegalArgumentException.class, () -> p.setIsActive(null));
    }

    /**
     * Protege el sistema de ediciones que intenten dejar el producto sin denominación.
     */
    @Test
    @DisplayName("setName vacio lanza excepcion")
    void setNameVacioLanzaExcepcion() {
        Product p = validProduct();
        assertThrows(IllegalArgumentException.class, () -> p.setName(""));
    }

}
