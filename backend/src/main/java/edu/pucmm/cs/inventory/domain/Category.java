package edu.pucmm.cs.inventory.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

/**
 * Entidad JPA que representa una categoría de productos.
 * Mapea a la tabla 'categories'.
 */
@Entity
@Table(name = "categories")
public class Category {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "name", nullable = false, unique = true, length = 150)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    /** Constructor sin argumentos requerido por JPA. */
    protected Category() {
    }

    /** Constructor con validaciones de negocio. */
    public Category(UUID id, String name, String description) {
        if (id == null) {
            throw new IllegalArgumentException("El ID de la categoría no puede ser nulo.");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre de la categoría no puede estar vacío.");
        }
        this.id = id;
        this.name = name;
        this.description = description;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre de la categoría no puede estar vacío.");
        }
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }
    
}
