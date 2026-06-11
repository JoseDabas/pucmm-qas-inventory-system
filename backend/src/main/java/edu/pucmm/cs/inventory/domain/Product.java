package edu.pucmm.cs.inventory.domain;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Entidad de dominio que representa un Producto en el sistema de inventario.
 * 
 * Esta clase forma parte de la capa de dominio (Domain Layer) siguiendo Clean
 * Architecture
 * y Domain-Driven Design (DDD). Es un POJO (Plain Old Java Object) puro y no
 * tiene
 * ninguna dependencia con frameworks externos (como Spring o JPA).
 * 
 * Su propósito es encapsular el estado de un producto y aplicar las reglas de
 * negocio
 * y validaciones fundamentales (invariantes) en el momento de su creación o
 * modificación.
 */
public class Product {

    // Identificador único universal del producto
    private UUID id;

    // Nombre descriptivo del producto
    private String name;

    // Código SKU (Stock Keeping Unit) único para identificar el producto en el
    // inventario
    private String skuCode;

    // Descripción detallada de las características del producto
    private String description;

    // Categoría a la que pertenece el producto
    private String category;

    // Precio unitario del producto
    private BigDecimal price;

    // Cantidad inicial del producto al ser registrado
    private Integer initialQuantity;

    // Nivel mínimo de stock permitido para generar alertas
    private Integer minimumStock;

    // Estado del producto: indica si está activo o inactivo en el sistema
    private Boolean isActive;

    /**
     * Constructor completo de la clase Product.
     * 
     * Este constructor se encarga de instanciar un nuevo producto y, de forma
     * crucial,
     * aplica las reglas de negocio base garantizando que un producto no pueda ser
     * creado en un estado inválido.
     *
     * @param id              Identificador del producto.
     * @param name            Nombre del producto (no puede ser nulo o vacío).
     * @param skuCode         Código SKU (no puede ser nulo o vacío).
     * @param description     Descripción del producto.
     * @param category        Categoría del producto.
     * @param price           Precio (debe ser mayor o igual a cero).
     * @param initialQuantity Cantidad inicial (debe ser mayor o igual a cero).
     * @param minimumStock    Stock mínimo (debe ser mayor o igual a cero).
     * @param isActive        Estado del producto.
     */
    public Product(UUID id, String name, String skuCode, String description, String category,
            BigDecimal price, Integer initialQuantity, Integer minimumStock, Boolean isActive) {

        // Validación del ID: no puede ser nulo
        if (id == null) {
            throw new IllegalArgumentException("El ID del producto no puede ser nulo.");
        }

        // Validación del nombre: no puede ser nulo ni estar en blanco
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del producto no puede estar vacío.");
        }

        // Validación del SKU: no puede ser nulo ni estar en blanco
        if (skuCode == null || skuCode.trim().isEmpty()) {
            throw new IllegalArgumentException("El código SKU no puede estar vacío.");
        }

        // Validación del precio: no puede ser nulo ni menor que cero (regla de negocio)
        if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El precio del producto no puede ser negativo o nulo.");
        }

        // Validación de la cantidad inicial: no puede ser nula ni menor que cero (regla
        // de negocio)
        if (initialQuantity == null || initialQuantity < 0) {
            throw new IllegalArgumentException("La cantidad inicial no puede ser negativa o nula.");
        }

        // Validación del stock mínimo: no puede ser nulo ni menor que cero (regla de
        // negocio)
        if (minimumStock == null || minimumStock < 0) {
            throw new IllegalArgumentException("El stock mínimo no puede ser negativo o nulo.");
        }

        // Asignación de valores una vez han pasado todas las validaciones
        this.id = id;
        this.name = name;
        this.skuCode = skuCode;
        this.description = description;
        this.category = category;
        this.price = price;
        this.initialQuantity = initialQuantity;
        this.minimumStock = minimumStock;
        // Si no se especifica el estado activo, por defecto será true
        this.isActive = isActive != null ? isActive : true;
    }

    // ==========================================
    // GETTERS
    // Proveen acceso de solo lectura a los atributos de la entidad
    // ==========================================

    /** @return El identificador del producto. */
    public UUID getId() {
        return id;
    }

    /** @return El nombre del producto. */
    public String getName() {
        return name;
    }

    /** @return El código SKU del producto. */
    public String getSkuCode() {
        return skuCode;
    }

    /** @return La descripción del producto. */
    public String getDescription() {
        return description;
    }

    /** @return La categoría del producto. */
    public String getCategory() {
        return category;
    }

    /** @return El precio del producto. */
    public BigDecimal getPrice() {
        return price;
    }

    /** @return La cantidad inicial del producto. */
    public Integer getInitialQuantity() {
        return initialQuantity;
    }

    /** @return El stock mínimo permitido. */
    public Integer getMinimumStock() {
        return minimumStock;
    }

    /** @return El estado del producto (activo/inactivo). */
    public Boolean getIsActive() {
        return isActive;
    }

    // ==========================================
    // SETTERS CON VALIDACIONES DE NEGOCIO
    // Permiten modificar el estado garantizando la coherencia de la entidad
    // ==========================================

    /**
     * Actualiza el precio del producto validando que no sea negativo.
     * 
     * @param price Nuevo precio a establecer.
     */
    public void setPrice(BigDecimal price) {
        if (price == null || price.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("El precio actualizado no puede ser negativo o nulo.");
        }
        this.price = price;
    }

    /**
     * Actualiza el stock mínimo validando que no sea negativo.
     * 
     * @param minimumStock Nuevo stock mínimo.
     */
    public void setMinimumStock(Integer minimumStock) {
        if (minimumStock == null || minimumStock < 0) {
            throw new IllegalArgumentException("El stock mínimo actualizado no puede ser negativo o nulo.");
        }
        this.minimumStock = minimumStock;
    }

    /**
     * Cambia el estado del producto (activo o inactivo).
     * 
     * @param isActive Nuevo estado.
     */
    public void setIsActive(Boolean isActive) {
        if (isActive == null) {
            throw new IllegalArgumentException("El estado de actividad no puede ser nulo.");
        }
        this.isActive = isActive;
    }
}
