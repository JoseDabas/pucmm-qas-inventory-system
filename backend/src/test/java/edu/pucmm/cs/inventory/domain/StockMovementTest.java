package edu.pucmm.cs.inventory.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class StockMovementTest {

    @Test
    @DisplayName("Debe crear un movimiento de stock válido y retornar sus valores correctamente")
    void shouldCreateValidStockMovement() {
        UUID id = UUID.randomUUID();
        UUID productId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        StockMovement movement = new StockMovement(
                id,
                productId,
                MovementType.IN,
                10,
                20,
                now,
                "admin",
                "Ingreso por compra"
        );

        assertEquals(id, movement.getId());
        assertEquals(productId, movement.getProductId());
        assertEquals(MovementType.IN, movement.getMovementType());
        assertEquals(10, movement.getPreviousQuantity());
        assertEquals(20, movement.getNewQuantity());
        assertEquals(now, movement.getDate());
        assertEquals("admin", movement.getUsername());
        assertEquals("Ingreso por compra", movement.getObservations());
    }

    @Test
    @DisplayName("Debe lanzar excepción si el id es nulo")
    void shouldThrowExceptionWhenIdIsNull() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> new StockMovement(null, UUID.randomUUID(), MovementType.IN, 0, 10, LocalDateTime.now(), "admin", "Obs")
        );
        assertTrue(ex.getMessage().contains("ID del movimiento"));
    }

    @Test
    @DisplayName("Debe lanzar excepción si el productId es nulo")
    void shouldThrowExceptionWhenProductIdIsNull() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> new StockMovement(UUID.randomUUID(), null, MovementType.IN, 0, 10, LocalDateTime.now(), "admin", "Obs")
        );
        assertTrue(ex.getMessage().contains("ID del producto"));
    }

    @Test
    @DisplayName("Debe lanzar excepción si el movementType es nulo")
    void shouldThrowExceptionWhenMovementTypeIsNull() {
        IllegalArgumentException ex = assertThrows(
                IllegalArgumentException.class,
                () -> new StockMovement(UUID.randomUUID(), UUID.randomUUID(), null, 0, 10, LocalDateTime.now(), "admin", "Obs")
        );
        assertTrue(ex.getMessage().contains("tipo de movimiento"));
    }

    @Test
    @DisplayName("Debe lanzar excepción si previousQuantity es nulo o negativo")
    void shouldThrowExceptionWhenPreviousQuantityIsInvalid() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new StockMovement(UUID.randomUUID(), UUID.randomUUID(), MovementType.IN, null, 10, LocalDateTime.now(), "admin", "Obs")
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new StockMovement(UUID.randomUUID(), UUID.randomUUID(), MovementType.IN, -1, 10, LocalDateTime.now(), "admin", "Obs")
        );
    }

    @Test
    @DisplayName("Debe lanzar excepción si newQuantity es nulo o negativo")
    void shouldThrowExceptionWhenNewQuantityIsInvalid() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new StockMovement(UUID.randomUUID(), UUID.randomUUID(), MovementType.IN, 10, null, LocalDateTime.now(), "admin", "Obs")
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new StockMovement(UUID.randomUUID(), UUID.randomUUID(), MovementType.IN, 10, -5, LocalDateTime.now(), "admin", "Obs")
        );
    }

    @Test
    @DisplayName("Debe lanzar excepción si date es nula")
    void shouldThrowExceptionWhenDateIsNull() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new StockMovement(UUID.randomUUID(), UUID.randomUUID(), MovementType.IN, 0, 10, null, "admin", "Obs")
        );
    }

    @Test
    @DisplayName("Debe lanzar excepción si username es nulo o vacío")
    void shouldThrowExceptionWhenUsernameIsInvalid() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new StockMovement(UUID.randomUUID(), UUID.randomUUID(), MovementType.IN, 0, 10, LocalDateTime.now(), null, "Obs")
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> new StockMovement(UUID.randomUUID(), UUID.randomUUID(), MovementType.IN, 0, 10, LocalDateTime.now(), "   ", "Obs")
        );
    }
}
