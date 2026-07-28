package edu.pucmm.cs.inventory.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * Entidad JPA que representa un Producto en el sistema de inventario.
 * Mapea a la tabla 'products'. Conserva las validaciones de negocio
 */
@Entity
@Table(name = "products")
public class Product {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "sku_code", nullable = false, unique = true, length = 100)
    private String skuCode;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    // Relación con Category vía FK category_id (reemplaza el antiguo String
    // category).
    @ManyToOne
    @JoinColumn(name = "category_id")
    private Category category;

    @Column(name = "price", nullable = false, precision = 19, scale = 4)
    private BigDecimal price;

    @Column(name = "initial_quantity", nullable = false)
    private Integer initialQuantity;

    @Column(name = "minimum_stock", nullable = false)
    private Integer minimumStock;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    /**
     * Constructor sin argumentos requerido por JPA. No usar directamente en código
     * de negocio.
     */
    protected Product() {
    }

    /** Constructor con validaciones de negocio (invariantes). */
    public Product(UUID id, String name, String skuCode, String description, Category category,
            BigDecimal price, Integer initialQuantity,
            Integer minimumStock, Boolean isActive) {

        if (id == null) {
            throw new IllegalArgumentException("El ID del producto no puede ser nulo.");
        }
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del producto no puede estar vacío.");
        }
        if (skuCode == null || skuCode.trim().isEmpty()) {
            throw new IllegalArgumentException("El código SKU no puede estar vacío.");
        }
        if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El precio del producto no puede ser negativo o nulo.");
        }
        if (initialQuantity == null || initialQuantity < 0) {
            throw new IllegalArgumentException("La cantidad inicial no puede ser negativa o nula.");
        }
        if (minimumStock == null || minimumStock < 0) {
            throw new IllegalArgumentException("El stock mínimo no puede ser negativo o nulo.");
        }

        this.id = id;
        this.name = name;
        this.skuCode = skuCode;
        this.description = description;
        this.category = category;
        this.price = price;
        this.initialQuantity = initialQuantity;
        this.minimumStock = minimumStock;
        this.isActive = isActive != null ? isActive : true;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getSkuCode() {
        return skuCode;
    }

    public String getDescription() {
        return description;
    }

    public Category getCategory() {
        return category;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public Integer getInitialQuantity() {
        return initialQuantity;
    }

    public Integer getMinimumStock() {
        return minimumStock;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del producto no puede estar vacío.");
        }
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public void setPrice(BigDecimal price) {
        if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El precio actualizado no puede ser negativo o nulo.");
        }
        this.price = price;
    }

    public void setMinimumStock(Integer minimumStock) {
        if (minimumStock == null || minimumStock < 0) {
            throw new IllegalArgumentException("El stock mínimo actualizado no puede ser negativo o nulo.");
        }
        this.minimumStock = minimumStock;
    }

    public void setIsActive(Boolean isActive) {
        if (isActive == null) {
            throw new IllegalArgumentException("El estado de actividad no puede ser nulo.");
        }
        this.isActive = isActive;
    }
}