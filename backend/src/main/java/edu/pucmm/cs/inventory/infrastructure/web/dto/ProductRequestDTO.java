package edu.pucmm.cs.inventory.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * DTO (Data Transfer Object) de entrada para las peticiones de Productos.
 * 
 * Se utiliza para encapsular los datos enviados por el cliente al crear o actualizar
 * un producto, garantizando la separación entre la capa de presentación y el dominio.
 * Incluye validaciones JSR-380 (Bean Validation) y documentación OpenAPI.
 */
@Schema(description = "Objeto de transferencia de datos para la creación o actualización de un Producto.")
public class ProductRequestDTO {

    // Longitud contada por puntos de código en el propio patrón (Java regex cuenta
    // el '.' por code point), consistente con maxLength de OpenAPI y VARCHAR(255)
    // de PostgreSQL; evita el desajuste UTF-16 de @Size con emojis/símbolos.
    @Schema(description = "Nombre descriptivo del producto", example = "Laptop Dell XPS 15", minLength = 1, maxLength = 255)
    @NotBlank(message = "El nombre es obligatorio")
    @jakarta.validation.constraints.Pattern(regexp = "(?s)(?=.*[a-zA-Z0-9]).{1,255}", message = "El nombre debe tener entre 1 y 255 caracteres e incluir al menos uno alfanumérico")
    private String name;

    @Schema(description = "Código SKU (Stock Keeping Unit) único", example = "LAP-DELL-XPS15", minLength = 1, maxLength = 100)
    @NotBlank(message = "El código SKU es obligatorio")
    @jakarta.validation.constraints.Pattern(regexp = "(?s)(?=.*[a-zA-Z0-9]).{1,100}", message = "El código SKU debe tener entre 1 y 100 caracteres e incluir al menos uno alfanumérico")
    private String skuCode;

    @Schema(description = "Descripción detallada de las características", example = "Laptop de 15 pulgadas, 16GB RAM")
    @NotNull(message = "La descripción no puede ser nula")
    private String description;

    @Schema(description = "Categoría de clasificación del producto", example = "Electrónica")
    @NotNull(message = "La categoría no puede ser nula")
    private String category;

    @Schema(description = "Precio unitario de venta", example = "1500.00", maximum = "100000000")
    @NotNull(message = "El precio es obligatorio")
    @Min(value = 0, message = "El precio no puede ser negativo")
    @jakarta.validation.constraints.Max(value = 100000000, message = "El precio excede el límite permitido")
    private BigDecimal price;

    @Schema(description = "Cantidad inicial del producto al ser registrado. Representará el primer movimiento de stock.", example = "50", maximum = "1000000")
    @NotNull(message = "La cantidad inicial es obligatoria")
    @Min(value = 0, message = "La cantidad inicial no puede ser negativa")
    @jakarta.validation.constraints.Max(value = 1000000, message = "La cantidad inicial excede el límite permitido")
    private Integer initialQuantity;

    @Schema(description = "Umbral de stock mínimo para generar alertas", example = "5", maximum = "1000000")
    @NotNull(message = "El stock mínimo es obligatorio")
    @Min(value = 0, message = "El stock mínimo no puede ser negativo")
    @jakarta.validation.constraints.Max(value = 1000000, message = "El stock mínimo excede el límite permitido")
    private Integer minimumStock;

    @Schema(description = "Estado del producto (activo/inactivo). Obligatorio: debe enviarse en cada petición.", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "El estado (activo/inactivo) es obligatorio")
    private Boolean isActive;

    // Getters
    public String getName() { return name; }
    public String getSkuCode() { return skuCode; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public BigDecimal getPrice() { return price; }
    public Integer getInitialQuantity() { return initialQuantity; }
    public Integer getMinimumStock() { return minimumStock; }
    public Boolean getIsActive() { return isActive; }

    // Setters
    public void setName(String name) { this.name = name; }
    public void setSkuCode(String skuCode) { this.skuCode = skuCode; }
    public void setDescription(String description) { this.description = description; }
    public void setCategory(String category) { this.category = category; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public void setInitialQuantity(Integer initialQuantity) { this.initialQuantity = initialQuantity; }
    public void setMinimumStock(Integer minimumStock) { this.minimumStock = minimumStock; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
}
