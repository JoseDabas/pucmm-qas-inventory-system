package edu.pucmm.cs.inventory.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

/**
 * DTO de salida que representa una categoría junto con la cantidad de productos
 * que la referencian, para alimentar la tabla del frontend.
 */
@Schema(description = "Objeto de transferencia de datos de respuesta que representa una Categoría.")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class CategoryResponseDTO {

    @Schema(description = "Identificador único de la categoría", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID id;

    @Schema(description = "Nombre de la categoría", example = "Electrónica")
    private String name;

    @Schema(description = "Descripción de la categoría", example = "Dispositivos y componentes electrónicos", nullable = true)
    private String description;

    @Schema(description = "Cantidad de productos asociados a esta categoría", example = "12")
    private Integer productCount;

    // Getters
    public UUID getId() { return id; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public Integer getProductCount() { return productCount; }

    // Setters
    public void setId(UUID id) { this.id = id; }
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
    public void setProductCount(Integer productCount) { this.productCount = productCount; }
}
