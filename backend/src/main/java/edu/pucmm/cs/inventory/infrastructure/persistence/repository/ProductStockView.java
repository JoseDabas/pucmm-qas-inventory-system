package edu.pucmm.cs.inventory.infrastructure.persistence.repository;

import java.util.UUID;

/**
 * Proyección de Spring Data para exponer el stock actual de un producto calculado
 * desde el ledger de movimientos (SUM de entradas menos salidas). Se usa en la
 * consulta por lote para evitar el problema N+1 al listar productos.
 */
public interface ProductStockView {

    UUID getProductId();

    Long getTotal();
}
