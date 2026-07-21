package edu.pucmm.cs.inventory.infrastructure.web.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

/**
 * DTO (Data Transfer Object) de salida para el ranking de productos más vendidos.
 *
 * Expone, por producto, el total histórico de unidades salidas (movimientos OUT),
 * evitando acoplar las entidades JPA directamente a las respuestas HTTP.
 */
@Schema(description = "Producto dentro del ranking de más vendidos, con el total de unidades salidas.")
public class TopSellingProductResponseDTO {

    @Schema(description = "Identificador único universal del producto", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID productId;

    @Schema(description = "Nombre del producto", example = "Laptop Dell XPS 15")
    private String productName;

    @Schema(description = "Código SKU del producto", example = "LAP-DELL-XPS15")
    private String skuCode;

    @Schema(description = "Total histórico de unidades salidas (suma de movimientos OUT)", example = "128")
    private Long totalOut;

    public TopSellingProductResponseDTO() {
    }

    public TopSellingProductResponseDTO(UUID productId, String productName, String skuCode, Long totalOut) {
        this.productId = productId;
        this.productName = productName;
        this.skuCode = skuCode;
        this.totalOut = totalOut;
    }

    // Getters
    public UUID getProductId() { return productId; }
    public String getProductName() { return productName; }
    public String getSkuCode() { return skuCode; }
    public Long getTotalOut() { return totalOut; }

    // Setters
    public void setProductId(UUID productId) { this.productId = productId; }
    public void setProductName(String productName) { this.productName = productName; }
    public void setSkuCode(String skuCode) { this.skuCode = skuCode; }
    public void setTotalOut(Long totalOut) { this.totalOut = totalOut; }
}
