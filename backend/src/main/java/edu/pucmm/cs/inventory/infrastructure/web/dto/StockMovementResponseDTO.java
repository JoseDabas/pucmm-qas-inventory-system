package edu.pucmm.cs.inventory.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO (Data Transfer Object) de salida que representa una fila del Historial de
 * Movimientos, con la información que consume directamente la tabla del frontend:
 * Fecha, Usuario, Tipo de movimiento, Cantidad anterior, Cantidad nueva y Observaciones.
 */
@Schema(description = "Objeto de transferencia de datos de respuesta que representa un movimiento de stock.")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StockMovementResponseDTO {

    @Schema(description = "Identificador único del movimiento", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID id;

    @Schema(description = "Identificador del producto afectado", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID productId;

    @Schema(description = "Nombre del producto afectado", example = "Laptop Dell XPS 15", nullable = true)
    private String productName;

    @Schema(description = "Tipo de movimiento: IN (entrada) u OUT (salida)", example = "IN")
    private String movementType;

    @Schema(description = "Cantidad de unidades movidas", example = "10")
    private Integer quantity;

    @Schema(description = "Stock del producto antes del movimiento", example = "40")
    private Integer previousQuantity;

    @Schema(description = "Stock del producto después del movimiento", example = "50")
    private Integer newQuantity;

    @Schema(description = "Fecha y hora del movimiento", example = "2026-07-18T14:30:00-04:00")
    private OffsetDateTime date;

    @Schema(description = "Usuario que realizó el movimiento", example = "jariel")
    private String username;

    @Schema(description = "Observaciones del movimiento", example = "Reposición de inventario", nullable = true)
    private String observations;

    // Getters
    public UUID getId() { return id; }
    public UUID getProductId() { return productId; }
    public String getProductName() { return productName; }
    public String getMovementType() { return movementType; }
    public Integer getQuantity() { return quantity; }
    public Integer getPreviousQuantity() { return previousQuantity; }
    public Integer getNewQuantity() { return newQuantity; }
    public OffsetDateTime getDate() { return date; }
    public String getUsername() { return username; }
    public String getObservations() { return observations; }

    // Setters
    public void setId(UUID id) { this.id = id; }
    public void setProductId(UUID productId) { this.productId = productId; }
    public void setProductName(String productName) { this.productName = productName; }
    public void setMovementType(String movementType) { this.movementType = movementType; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public void setPreviousQuantity(Integer previousQuantity) { this.previousQuantity = previousQuantity; }
    public void setNewQuantity(Integer newQuantity) { this.newQuantity = newQuantity; }
    public void setDate(OffsetDateTime date) { this.date = date; }
    public void setUsername(String username) { this.username = username; }
    public void setObservations(String observations) { this.observations = observations; }
}
