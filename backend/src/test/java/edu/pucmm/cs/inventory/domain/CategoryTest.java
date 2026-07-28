package edu.pucmm.cs.inventory.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias para la entidad de dominio Category.
 * Verifica la correcta instanciación de la categoría y la validación
 * obligatoria de sus atributos para preservar la consistencia de los datos.
 */
class CategoryTest {

    /**
     * Comprueba la creación exitosa de una categoría cuando
     * se proporcionan todos los campos requeridos de forma correcta.
     */
    @Test
    @DisplayName("Crea categoria valida")
    void creaCategoriaValida() {
        Category c = new Category(UUID.randomUUID(), "Electronica", "Productos electronicos");
        assertEquals("Electronica", c.getName());
        assertEquals("Productos electronicos", c.getDescription());
    }

    /**
     * Verifica que el constructor rechace la inicialización
     * si el identificador único (ID) no es provisto.
     */
    @Test
    @DisplayName("ID nulo lanza excepcion")
    void idNuloLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class,
                () -> new Category(null, "Electronica", null));
    }

    /**
     * Asegura que el dominio no permita categorías sin nombre,
     * ya que es un atributo indispensable para su identificación.
     */
    @Test
    @DisplayName("Nombre nulo lanza excepcion")
    void nombreNuloLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class,
                () -> new Category(UUID.randomUUID(), null, null));
    }

    /**
     * Valida que el nombre de la categoría no contenga únicamente espacios en blanco,
     * previniendo registros inválidos o invisibles.
     */
    @Test
    @DisplayName("Nombre vacio lanza excepcion")
    void nombreVacioLanzaExcepcion() {
        assertThrows(IllegalArgumentException.class,
                () -> new Category(UUID.randomUUID(), "   ", null));
    }

    /**
     * Comprueba que se pueda actualizar el nombre de una categoría 
     * satisfactoriamente si el nuevo valor cumple las reglas de validación.
     */
    @Test
    @DisplayName("setName valido actualiza")
    void setNameValidoActualiza() {
        Category c = new Category(UUID.randomUUID(), "Electronica", null);
        c.setName("Hogar");
        assertEquals("Hogar", c.getName());
    }

    /**
     * Verifica que el setter de nombre ejecute las mismas validaciones de seguridad
     * que el constructor, rechazando asignaciones de texto vacío.
     */
    @Test
    @DisplayName("setName vacio lanza excepcion")
    void setNameVacioLanzaExcepcion() {
        Category c = new Category(UUID.randomUUID(), "Electronica", null);
        assertThrows(IllegalArgumentException.class, () -> c.setName(""));
    }

    /**
     * Asegura que el atributo de descripción, al ser opcional, 
     * permita actualizaciones sin aplicar restricciones estrictas.
     */
    @Test
    @DisplayName("setDescription actualiza sin validacion")
    void setDescriptionActualiza() {
        Category c = new Category(UUID.randomUUID(), "Electronica", null);
        c.setDescription("Nueva desc");
        assertEquals("Nueva desc", c.getDescription());
    }

    /**
     * Valida que la entidad no intente autogenerar fechas de creación (createdAt),
     * sino que delegue esta responsabilidad al ORM (Hibernate) 
     * al momento de persistirse en la base de datos.
     */
    @Test
    @DisplayName("createdAt es nulo hasta que Hibernate lo asigna al persistir")
    void createdAtNuloAntesDePersistir() {
        Category c = new Category(UUID.randomUUID(), "Electronica", null);
        // La marca de tiempo la fija @CreationTimestamp en el INSERT, no el constructor.
        assertNull(c.getCreatedAt());
    }

}
