package edu.pucmm.cs.inventory.infrastructure.persistence.entity;

import edu.pucmm.cs.inventory.domain.Category;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.envers.Audited;
import org.hibernate.envers.RelationTargetAuditMode;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Entidad JPA que representa la tabla 'products' en la base de datos.
 * 
 * Esta clase reside en la capa de infraestructura. Su responsabilidad exclusiva
 * es definir el mapeo Objeto-Relacional (ORM) entre la base de datos PostgreSQL
 * y la aplicación Spring Boot, sin contener ninguna lógica de negocio.
 */
@Entity // Indica a Hibernate que esta clase es una entidad persistente gestionada por
        // el EntityManager
@Table(name = "products") // Especifica el nombre exacto de la tabla en la base de datos a mapear
@Audited // Hibernate Envers: Habilita el control de versiones y auditoría de cambios
         // para esta entidad en la tabla 'products_aud'
public class ProductEntity {

    @Id // Marca este campo como la clave primaria (Primary Key) de la tabla
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "sku_code", nullable = false, unique = true)
    private String skuCode;

    // Almacenado como un tipo de texto largo en base de datos
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    // Relación con la categoría vía FK category_id hacia la tabla 'categories'.
    // La auditoría de Envers no rastrea la entidad relacionada (solo el FK).
    @ManyToOne
    @JoinColumn(name = "category_id")
    @Audited(targetAuditMode = RelationTargetAuditMode.NOT_AUDITED)
    private Category category;

    // Definimos la precisión a nivel de base de datos para manejar valores
    // monetarios
    @Column(name = "price", nullable = false, precision = 19, scale = 4)
    private BigDecimal price;

    @Column(name = "initial_quantity", nullable = false)
    private Integer initialQuantity;

    @Column(name = "minimum_stock", nullable = false)
    private Integer minimumStock;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive;

    /**
     * Constructor público sin argumentos.
     * Es un requisito estricto de JPA/Hibernate para poder instanciar la entidad
     * mediante reflexión durante las operaciones de base de datos.
     */
    public ProductEntity() {
    }

    // ==========================================
    // GETTERS Y SETTERS
    // Necesarios para que JPA/Hibernate pueda mutar y acceder al estado de la
    // entidad
    // ==========================================

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSkuCode() {
        return skuCode;
    }

    public void setSkuCode(String skuCode) {
        this.skuCode = skuCode;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Integer getInitialQuantity() {
        return initialQuantity;
    }

    public void setInitialQuantity(Integer initialQuantity) {
        this.initialQuantity = initialQuantity;
    }

    public Integer getMinimumStock() {
        return minimumStock;
    }

    public void setMinimumStock(Integer minimumStock) {
        this.minimumStock = minimumStock;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}
