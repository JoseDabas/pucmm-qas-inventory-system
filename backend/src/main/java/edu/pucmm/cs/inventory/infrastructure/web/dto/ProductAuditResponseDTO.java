package edu.pucmm.cs.inventory.infrastructure.web.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * DTO de salida que representa una revisión de auditoría de un producto generada por
 * Hibernate Envers. Cada instancia corresponde a un cambio (alta/modificación/baja)
 * registrado en la tabla 'products_aud', con la foto del producto en ese punto del tiempo.
 *
 * registrada en la tabla de auditoría, con la foto del producto en ese punto del tiempo.
 * Se incluye el usuario que realizó el cambio desde la entidad de revisión.
 */
@Schema(description = "Revisión de auditoría (Hibernate Envers) de un producto en un punto del tiempo.")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ProductAuditResponseDTO {

    @Schema(description = "Número de revisión asignado por Envers", example = "12")
    private Integer revision;

    @Schema(description = "ID original del producto", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID entityId;

    @Schema(description = "Tipo de cambio: CREATED (alta), UPDATED (modificación) o DELETED (baja)", example = "UPDATED")
    private String revisionType;

    @Schema(description = "Fecha y hora en que se registró la revisión", example = "2026-07-18T14:30:00-04:00")
    private OffsetDateTime revisionDate;

    @Schema(description = "Nombre del producto en esta revisión", example = "Laptop Dell XPS 15", nullable = true)
    private String name;

    @Schema(description = "Código SKU en esta revisión", example = "LAP-DELL-XPS15", nullable = true)
    private String skuCode;

    @Schema(description = "Descripción en esta revisión", nullable = true)
    private String description;

    @Schema(description = "Categoría en esta revisión", example = "Electrónica", nullable = true)
    private String category;

    @Schema(description = "Precio en esta revisión", example = "1500.00", nullable = true)
    private BigDecimal price;

    @Schema(description = "Cantidad inicial en esta revisión", example = "50", nullable = true)
    private Integer initialQuantity;

    @Schema(description = "Stock mínimo en esta revisión", example = "5", nullable = true)
    private Integer minimumStock;

    @Schema(description = "Estado activo/inactivo en esta revisión", example = "true", nullable = true)
    private Boolean isActive;

    @Schema(description = "Usuario que realizó el cambio", example = "admin_user", nullable = true)
    private String modifiedBy;

    // Getters
    public Integer getRevision() { return revision; }
    public UUID getEntityId() { return entityId; }
    public String getRevisionType() { return revisionType; }
    public OffsetDateTime getRevisionDate() { return revisionDate; }
    public String getName() { return name; }
    public String getSkuCode() { return skuCode; }
    public String getDescription() { return description; }
    public String getCategory() { return category; }
    public BigDecimal getPrice() { return price; }
    public Integer getInitialQuantity() { return initialQuantity; }
    public Integer getMinimumStock() { return minimumStock; }
    public Boolean getIsActive() { return isActive; }
    public String getModifiedBy() { return modifiedBy; }

    // Setters
    public void setRevision(Integer revision) { this.revision = revision; }
    public void setEntityId(UUID entityId) { this.entityId = entityId; }
    public void setRevisionType(String revisionType) { this.revisionType = revisionType; }
    public void setRevisionDate(OffsetDateTime revisionDate) { this.revisionDate = revisionDate; }
    public void setName(String name) { this.name = name; }
    public void setSkuCode(String skuCode) { this.skuCode = skuCode; }
    public void setDescription(String description) { this.description = description; }
    public void setCategory(String category) { this.category = category; }
    public void setPrice(BigDecimal price) { this.price = price; }
    public void setInitialQuantity(Integer initialQuantity) { this.initialQuantity = initialQuantity; }
    public void setMinimumStock(Integer minimumStock) { this.minimumStock = minimumStock; }
    public void setIsActive(Boolean isActive) { this.isActive = isActive; }
    public void setModifiedBy(String modifiedBy) { this.modifiedBy = modifiedBy; }
}
