package edu.pucmm.cs.inventory.infrastructure.persistence.repository;

import java.util.UUID;

/**
 * Proyección de Spring Data para exponer la cantidad de productos por categoría,
 * usada en la consulta por lote que alimenta la tabla de categorías (evita N+1).
 */
public interface CategoryProductCountView {

    UUID getCategoryId();

    Long getTotal();
}
