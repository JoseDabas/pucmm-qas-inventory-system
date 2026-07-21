package edu.pucmm.cs.inventory.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO de entrada para crear una categoría de productos.
 * Incluye validaciones JSR-380 (Bean Validation) y documentación OpenAPI para
 * mantener el contrato estricto verificado con Schemathesis.
 */
@Schema(description = "Objeto de transferencia de datos para la creación de una Categoría.")
public class CategoryRequestDTO {

    @Schema(description = "Nombre único de la categoría", example = "Electrónica", minLength = 1, maxLength = 150, requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "El nombre de la categoría es obligatorio")
    @Pattern(regexp = "(?s).*[a-zA-Z0-9].*", message = "Debe contener al menos un carácter alfanumérico")
    @Size(max = 150, message = "El nombre no puede exceder los 150 caracteres")
    private String name;

    @Schema(description = "Descripción opcional de la categoría", example = "Dispositivos y componentes electrónicos")
    private String description;

    // Getters
    public String getName() { return name; }
    public String getDescription() { return description; }

    // Setters
    public void setName(String name) { this.name = name; }
    public void setDescription(String description) { this.description = description; }
}
