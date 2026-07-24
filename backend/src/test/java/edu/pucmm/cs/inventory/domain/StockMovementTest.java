package edu.pucmm.cs.inventory.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class StockMovementTest {

    private final UUID validId = UUID.randomUUID();
    private final UUID validProductId = UUID.randomUUID();
    private final LocalDateTime validDate = LocalDateTime.now();

    @Test
    @DisplayName("Debe crear un movimiento de stock válido y retornar sus valores correctamente")
    void shouldCreateValidStockMovement() {
        StockMovement movement = new StockMovement(
                validId,
                validProductId,
                MovementType.IN,
                10,
                20,
                validDate,
                "admin",
                "Ingreso por compra"
        );

        assertEquals(validId, movement.getId());
        assertEquals(validProductId, movement.getProductId());
        assertEquals(MovementType.IN, movement.getMovementType());
        assertEquals(10, movement.getPreviousQuantity());
        assertEquals(20, movement.getNewQuantity());
        assertEquals(validDate, movement.getDate());
        assertEquals("admin", movement.getUsername());
        assertEquals("Ingreso por compra", movement.getObservations());
    }

    @Test
    @DisplayName("Debe lanzar excepción si el id es nulo")
    void shouldThrowExceptionWhenIdIsNull() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> new StockMovement(null, validProductId, MovementType.IN, 0, 10, validDate, "admin", "Obs")
        );
        assertTrue(ex.getMessage().contains("ID del movimiento"));
    }

    @Test
    @DisplayName("Debe lanzar excepción si el productId es nulo")
    void shouldThrowExceptionWhenProductIdIsNull() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> new StockMovement(validId, null, MovementType.IN, 0, 10, validDate, "admin", "Obs")
        );
        assertTrue(ex.getMessage().contains("ID del producto"));
    }

    @Test
    @DisplayName("Debe lanzar excepción si el movementType es nulo")
    void shouldThrowExceptionWhenMovementTypeIsNull() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> new StockMovement(validId, validProductId, null, 0, 10, validDate, "admin", "Obs")
        );
        assertTrue(ex.getMessage().contains("tipo de movimiento"));
    }

    @Test
    @DisplayName("Debe lanzar excepción si previousQuantity es nulo o negativo")
    void shouldThrowExceptionWhenPreviousQuantityIsInvalid() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new StockMovement(validId, validProductId, MovementType.IN, null, 10, validDate, "admin", "Obs")
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new StockMovement(validId, validProductId, MovementType.IN, -1, 10, validDate, "admin", "Obs")
        );
    }

    @Test
    @DisplayName("Debe lanzar excepción si newQuantity es nulo o negativo")
    void shouldThrowExceptionWhenNewQuantityIsInvalid() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new StockMovement(validId, validProductId, MovementType.IN, 10, null, validDate, "admin", "Obs")
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new StockMovement(validId, validProductId, MovementType.IN, 10, -5, validDate, "admin", "Obs")
        );
    }

    @Test
    @DisplayName("Debe lanzar excepción si date es nula")
    void shouldThrowExceptionWhenDateIsNull() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new StockMovement(validId, validProductId, MovementType.IN, 0, 10, null, "admin", "Obs")
        );
    }

    @Test
    @DisplayName("Debe lanzar excepción si username es nulo o vacío")
    void shouldThrowExceptionWhenUsernameIsInvalid() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new StockMovement(validId, validProductId, MovementType.IN, 0, 10, validDate, null, "Obs")
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new StockMovement(validId, validProductId, MovementType.IN, 0, 10, validDate, "   ", "Obs")
        );
    }
}
