package edu.pucmm.cs.inventory.infrastructure.persistence.repository;

import java.time.LocalDateTime;

/**
 * Proyección de Spring Data JPA para el reporte de movimientos de inventario.
 * Permite extraer solo los campos necesarios de StockMovementEntity y
 * ProductEntity/CategoryEntity de forma eficiente (sin traer el grafo completo).
 */
public interface MovementReportView {
    String getProductName();
    String getCategoryName();
    String getMovementType();
    Integer getPreviousQuantity();
    Integer getNewQuantity();
    LocalDateTime getDate();
    String getUsername();
}
