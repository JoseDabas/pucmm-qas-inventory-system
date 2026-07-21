package edu.pucmm.cs.inventory.infrastructure.persistence.repository;

import java.util.UUID;

/**
 * Proyección de Spring Data para exponer el ranking de productos más vendidos,
 * calculado desde el ledger de movimientos como la suma histórica de las salidas
 * (movimientos OUT) de cada producto. Se une con ProductEntity para traer el
 * nombre y el código SKU en la misma consulta y evitar el problema N+1.
 */
public interface TopSellingProductView {

    UUID getProductId();

    String getProductName();

    String getSkuCode();

    Long getTotalOut();
}
