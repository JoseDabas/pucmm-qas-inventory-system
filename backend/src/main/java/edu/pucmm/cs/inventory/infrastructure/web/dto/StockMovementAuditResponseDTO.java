package edu.pucmm.cs.inventory.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.OffsetDateTime;
import java.util.UUID;

@Schema(description = "Revisión de auditoría de un movimiento de stock en un punto del tiempo.")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class StockMovementAuditResponseDTO {

    @Schema(description = "Número de revisión asignado por Envers", example = "15")
    private Integer revision;

    @Schema(description = "ID original del movimiento de stock", example = "123e4567-e89b-12d3-a456-426614174001")
    private UUID entityId;

    @Schema(description = "Tipo de cambio: CREATED, UPDATED o DELETED", example = "CREATED")
    private String revisionType;

    @Schema(description = "Fecha y hora en que se registró la revisión")
    private OffsetDateTime revisionDate;

    @Schema(description = "ID del producto asociado")
    private UUID productId;

    @Schema(description = "Tipo de movimiento (IN/OUT/ADJUST)")
    private String movementType;

    @Schema(description = "Cantidad anterior")
    private Integer previousQuantity;

    @Schema(description = "Nueva cantidad")
    private Integer newQuantity;

    @Schema(description = "Fecha del movimiento")
    private OffsetDateTime movementDate;

    @Schema(description = "Usuario que realizó el movimiento")
    private String username;

    @Schema(description = "Observaciones")
    private String observations;

    @Schema(description = "Usuario del sistema que generó esta revisión")
    private String modifiedBy;

    // Getters
    public Integer getRevision() { return revision; }
    public UUID getEntityId() { return entityId; }
    public String getRevisionType() { return revisionType; }
    public OffsetDateTime getRevisionDate() { return revisionDate; }
    public UUID getProductId() { return productId; }
    public String getMovementType() { return movementType; }
    public Integer getPreviousQuantity() { return previousQuantity; }
    public Integer getNewQuantity() { return newQuantity; }
    public OffsetDateTime getMovementDate() { return movementDate; }
    public String getUsername() { return username; }
    public String getObservations() { return observations; }
    public String getModifiedBy() { return modifiedBy; }

    // Setters
    public void setRevision(Integer revision) { this.revision = revision; }
    public void setEntityId(UUID entityId) { this.entityId = entityId; }
    public void setRevisionType(String revisionType) { this.revisionType = revisionType; }
    public void setRevisionDate(OffsetDateTime revisionDate) { this.revisionDate = revisionDate; }
    public void setProductId(UUID productId) { this.productId = productId; }
    public void setMovementType(String movementType) { this.movementType = movementType; }
    public void setPreviousQuantity(Integer previousQuantity) { this.previousQuantity = previousQuantity; }
    public void setNewQuantity(Integer newQuantity) { this.newQuantity = newQuantity; }
    public void setMovementDate(OffsetDateTime movementDate) { this.movementDate = movementDate; }
    public void setUsername(String username) { this.username = username; }
    public void setObservations(String observations) { this.observations = observations; }
    public void setModifiedBy(String modifiedBy) { this.modifiedBy = modifiedBy; }
}
