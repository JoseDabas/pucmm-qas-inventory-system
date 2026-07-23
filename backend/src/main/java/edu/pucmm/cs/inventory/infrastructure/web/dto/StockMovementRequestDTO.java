package edu.pucmm.cs.inventory.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;

import edu.pucmm.cs.inventory.domain.MovementType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;

/**
 * DTO (Data Transfer Object) de entrada para registrar un movimiento de stock
 * (entrada o salida) sobre un producto existente.
 *
 * Incluye validaciones JSR-380 (Bean Validation) y documentación OpenAPI para
 * mantener el contrato estricto verificado con Schemathesis.
 */
@Schema(description = "Objeto de transferencia de datos para registrar una entrada o salida de stock.")
public class StockMovementRequestDTO {

    @Schema(description = "Identificador único del producto afectado", example = "123e4567-e89b-12d3-a456-426614174000", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "El identificador del producto es obligatorio")
    private UUID productId;

    @Schema(description = "Tipo de movimiento: IN (entrada) u OUT (salida)", example = "IN", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "El tipo de movimiento es obligatorio")
    private MovementType movementType;

    @Schema(description = "Cantidad de unidades a mover (siempre positiva)", example = "10", minimum = "1", maximum = "1000000", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad debe ser al menos 1")
    @Max(value = 1000000, message = "La cantidad excede el límite permitido")
    private Integer quantity;

    // Campo opcional: puede omitirse, pero si se envía explícitamente como null se
    // rechaza (Nulls.FAIL) para respetar el contrato OpenAPI (type: string).
    @Schema(description = "Observaciones o justificación del movimiento", example = "Reposición de inventario")
    @JsonSetter(nulls = Nulls.FAIL)
    private String observations;

    // Getters
    public UUID getProductId() { return productId; }
    public MovementType getMovementType() { return movementType; }
    public Integer getQuantity() { return quantity; }
    public String getObservations() { return observations; }

    // Setters
    public void setProductId(UUID productId) { this.productId = productId; }
    public void setMovementType(MovementType movementType) { this.movementType = movementType; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public void setObservations(String observations) { this.observations = observations; }
}
