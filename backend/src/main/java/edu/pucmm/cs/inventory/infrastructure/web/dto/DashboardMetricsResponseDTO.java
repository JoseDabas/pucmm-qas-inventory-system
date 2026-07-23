package edu.pucmm.cs.inventory.infrastructure.web.dto;

import java.math.BigDecimal;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO de salida con las métricas e indicadores del tablero de control.
 * Se sirve desde un endpoint agregado accesible para cualquier usuario
 * autenticado, de modo que todos los roles puedan ver los valores del dashboard
 * sin necesitar los permisos de las secciones operativas (las listas detalladas
 * siguen protegidas por sus permisos).
 */
@Schema(description = "Métricas e indicadores agregados del dashboard.")
public class DashboardMetricsResponseDTO {

    @Schema(description = "Cantidad total de productos registrados", example = "128")
    private long totalProducts;

    @Schema(description = "Unidades totales en inventario (suma del stock actual)", example = "3540")
    private long totalUnits;

    @Schema(description = "Valor total del inventario (precio × stock actual)", example = "154200.00")
    private BigDecimal totalValue;

    @Schema(description = "Cantidad total de categorías", example = "12")
    private long totalCategories;

    @Schema(description = "Cantidad total de movimientos de stock registrados", example = "874")
    private long totalMovements;

    @Schema(description = "Cantidad de productos en estado de stock crítico", example = "7")
    private long criticalStockCount;

    // Getters
    public long getTotalProducts() { return totalProducts; }
    public long getTotalUnits() { return totalUnits; }
    public BigDecimal getTotalValue() { return totalValue; }
    public long getTotalCategories() { return totalCategories; }
    public long getTotalMovements() { return totalMovements; }
    public long getCriticalStockCount() { return criticalStockCount; }

    // Setters
    public void setTotalProducts(long totalProducts) { this.totalProducts = totalProducts; }
    public void setTotalUnits(long totalUnits) { this.totalUnits = totalUnits; }
    public void setTotalValue(BigDecimal totalValue) { this.totalValue = totalValue; }
    public void setTotalCategories(long totalCategories) { this.totalCategories = totalCategories; }
    public void setTotalMovements(long totalMovements) { this.totalMovements = totalMovements; }
    public void setCriticalStockCount(long criticalStockCount) { this.criticalStockCount = criticalStockCount; }
}
