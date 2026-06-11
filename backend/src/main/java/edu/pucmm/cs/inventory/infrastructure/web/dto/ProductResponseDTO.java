package edu.pucmm.cs.inventory.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.util.UUID;

/**
 * DTO (Data Transfer Object) de salida para devolver información de Productos.
 * 
 * Se utiliza para exponer datos del dominio o infraestructura hacia los clientes de la API REST,
 * evitando acoplar las entidades JPA (ProductEntity) directamente a las respuestas HTTP.
 */
@Schema(description = "Objeto de transferencia de datos de respuesta que representa un Producto.")
public class ProductResponseDTO {

    @Schema(description = "Identificador único universal del producto", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID id;

    @Schema(description = "Nombre del producto", example = "Laptop Dell XPS 15")
    private String name;

    @Schema(description = "Código SKU", example = "LAP-DELL-XPS15")
    private String skuCode;

    @Schema(description = "Descripción del producto", example = "Laptop de 15 pulgadas, 16GB RAM")
    private String description;

    @Schema(description = "Categoría", example = "Electrónica")
    private String category;

    @Schema(description = "Precio unitario", example = "1500.00")
    private BigDecimal price;

    @Schema(description = "Stock mínimo configurado", example = "5")
    private Integer minimumStock;

    @Schema(description = "Indica si el producto está activo en el sistema", example = "true")
    private Boolean isActive;

    // Getters
    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getSkuCode() { return skuCode; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public BigDecimal getPrice() { return price; }
    public Integer getMinimumStock() { return minimumStock; }
    public Boolean getIsActive() { return isActive; }

    // Setters
    public void setId(UUID id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setSkuCode(String skuCode) { this.skuCode = skuCode; }
    public void setDescription(String description) { this.description = description; }
    public void setCategory(String category) { this.category = category; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public void setMinimumStock(Integer minimumStock) { this.minimumStock = minimumStock; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
}
