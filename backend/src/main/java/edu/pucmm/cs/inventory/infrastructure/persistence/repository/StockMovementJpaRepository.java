package edu.pucmm.cs.inventory.infrastructure.persistence.repository;

import edu.pucmm.cs.inventory.infrastructure.persistence.entity.StockMovementEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Interfaz de repositorio de Spring Data JPA para la entidad StockMovementEntity.
 *
 * Abstrae el acceso a datos para el historial de movimientos, permitiendo ejecutar
 * operaciones sobre la tabla 'stock_movements' sin necesidad de escribir SQL directamente.
 */
@Repository // Registra el repositorio como un Bean de Spring en el contexto de aplicación
public interface StockMovementJpaRepository extends JpaRepository<StockMovementEntity, UUID> {

    void deleteByProductId(UUID productId);

    /**
     * Calcula el stock actual de un producto a partir del ledger de movimientos:
     * suma las entradas (IN) y resta las salidas (OUT). El movimiento inicial ya
     * forma parte de este ledger, por lo que NO se debe sumar 'initial_quantity'
     * aparte (se evita el doble conteo). Devuelve 0 si el producto no tiene movimientos.
     */
    @Query("SELECT COALESCE(SUM(CASE WHEN m.movementType = 'IN' THEN m.quantity " +
           "WHEN m.movementType = 'OUT' THEN -m.quantity ELSE 0 END), 0) " +
           "FROM StockMovementEntity m WHERE m.productId = :productId")
    Integer sumSignedQuantityByProductId(@Param("productId") UUID productId);

    /**
     * Versión por lote de {@link #sumSignedQuantityByProductId}: calcula el stock actual
     * de varios productos en una sola consulta (evita el problema N+1 al listar). Los
     * productos sin movimientos no aparecen en el resultado y se interpretan como stock 0.
     */
    @Query("SELECT m.productId AS productId, " +
           "SUM(CASE WHEN m.movementType = 'IN' THEN m.quantity " +
           "WHEN m.movementType = 'OUT' THEN -m.quantity ELSE 0 END) AS total " +
           "FROM StockMovementEntity m WHERE m.productId IN :productIds GROUP BY m.productId")
    List<ProductStockView> sumSignedQuantitiesByProductIds(@Param("productIds") List<UUID> productIds);

    /**
     * Búsqueda paginada del historial filtrando por nombre de producto o usuario
     * (case-insensitive). Se usa un join implícito por 'productId' porque la entidad
     * de movimiento no mantiene una relación @ManyToOne hacia el producto.
     */
    @Query(value = "SELECT m FROM StockMovementEntity m, ProductEntity p " +
                   "WHERE m.productId = p.id AND (" +
                   "LOWER(p.name) LIKE LOWER(CONCAT('%', :term, '%')) OR " +
                   "LOWER(m.username) LIKE LOWER(CONCAT('%', :term, '%')))",
           countQuery = "SELECT COUNT(m) FROM StockMovementEntity m, ProductEntity p " +
                        "WHERE m.productId = p.id AND (" +
                        "LOWER(p.name) LIKE LOWER(CONCAT('%', :term, '%')) OR " +
                        "LOWER(m.username) LIKE LOWER(CONCAT('%', :term, '%')))")
    Page<StockMovementEntity> searchByProductNameOrUsername(@Param("term") String term, Pageable pageable);

    /**
     * Ranking de productos más vendidos: agrupa por producto y suma la cantidad
     * histórica de todas sus salidas (movimientos OUT), de mayor a menor. Se usa
     * un join implícito por 'productId' con ProductEntity para traer el nombre y
     * el SKU en la misma consulta (evita el problema N+1). El límite se controla
     * con el Pageable (p. ej. PageRequest.of(0, limit)).
     */
    @Query("SELECT p.id AS productId, p.name AS productName, p.skuCode AS skuCode, " +
           "SUM(m.quantity) AS totalOut " +
           "FROM StockMovementEntity m, ProductEntity p " +
           "WHERE m.productId = p.id AND m.movementType = 'OUT' " +
           "GROUP BY p.id, p.name, p.skuCode " +
           "ORDER BY SUM(m.quantity) DESC")
    List<TopSellingProductView> findTopSellingProducts(Pageable pageable);
}
