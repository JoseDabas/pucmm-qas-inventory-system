package edu.pucmm.cs.inventory.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

/**
 * DTO de entrada para crear una categoría de productos.
 * Incluye validaciones JSR-380 (Bean Validation) y documentación OpenAPI para
 * mantener el contrato estricto verificado con Schemathesis.
 */
@Schema(description = "Objeto de transferencia de datos para la creación de una Categoría.")
public class CategoryRequestDTO {

    @Schema(description = "Nombre único de la categoría", example = "Electrónica", minLength = 1, maxLength = 150, requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "El nombre de la categoría es obligatorio")
    @Pattern(regexp = "(?s)(?=.*[a-zA-Z0-9]).{1,150}", message = "El nombre debe tener entre 1 y 150 caracteres e incluir al menos uno alfanumérico")
    private String name;

    // Campo opcional: puede omitirse, pero si se envía explícitamente como null se
    // rechaza (Nulls.FAIL) para respetar el contrato OpenAPI (type: string).
    @Schema(description = "Descripción opcional de la categoría", example = "Dispositivos y componentes electrónicos")
    @JsonSetter(nulls = Nulls.FAIL)
    private String description;

    // Getters
    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    // Setters
    public void setName(String name) {
        this.name = name;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
