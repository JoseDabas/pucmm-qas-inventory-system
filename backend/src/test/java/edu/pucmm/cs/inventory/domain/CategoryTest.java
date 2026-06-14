package edu.pucmm.cs.inventory.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

public class CategoryTest {

    @Test
    @DisplayName("Crea categoria valida")
    void creaCategoriaValida() {
        Category c = new Category(UUID.randomUUID(), "Electronica", "Productos electronicos");
        assertEquals("Electronica", c.getName());
        assertEquals("Productos electronicos", c.getDescription());
    }

    @Test
    @DisplayName("ID nulo lanza excepcion")
    void idNuloLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class,
                () -> new Category(null, "Electronica", null));
    }

    @Test
    @DisplayName("Nombre nulo lanza excepcion")
    void nombreNuloLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class,
                () -> new Category(UUID.randomUUID(), null, null));
    }

    @Test
    @DisplayName("Nombre vacio lanza excepcion")
    void nombreVacioLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class,
                () -> new Category(UUID.randomUUID(), "   ", null));
    }

    @Test
    @DisplayName("setName valido actualiza")
    void setNameValidoActualiza() {
        Category c = new Category(UUID.randomUUID(), "Electronica", null);
        c.setName("Hogar");
        assertEquals("Hogar", c.getName());
    }

    @Test
    @DisplayName("setName vacio lanza excepcion")
    void setNameVacioLanzaExcepcion() {
        Category c = new Category(UUID.randomUUID(), "Electronica", null);
        assertThrows(IllegalArgumentException.class, () -> c.setName(""));
    }

    @Test
    @DisplayName("setDescription actualiza sin validacion")
    void setDescriptionActualiza() {
        Category c = new Category(UUID.randomUUID(), "Electronica", null);
        c.setDescription("Nueva desc");
        assertEquals("Nueva desc", c.getDescription());
    }

}
