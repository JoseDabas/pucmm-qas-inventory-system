package edu.pucmm.cs.inventory.infrastructure.persistence.repository;

import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import edu.pucmm.cs.inventory.infrastructure.persistence.entity.ProductEntity;

/**
 * Interfaz de repositorio de Spring Data JPA para la entidad ProductEntity.
 * 
 * Pertenece a la capa de infraestructura (Adaptadores de Persistencia).
 * Al extender JpaRepository, Spring genera automáticamente en tiempo de ejecución
 * la implementación para todas las operaciones CRUD (Create, Read, Update, Delete) 
 * y capacidades de paginación/ordenamiento para la tabla 'products'.
 */
@Repository // Marca esta interfaz como un componente administrado por el contenedor de Spring y habilita la traducción de excepciones de datos a DataAccessException
public interface ProductJpaRepository extends JpaRepository<ProductEntity, UUID> {

    /**
     * Selecciona productos activos y calcula su stock actual basándose en el historial de movimientos
     * para evitar condiciones de carrera, devolviendo aquellos con stock crítico.
     */
    @Query("SELECT p FROM ProductEntity p LEFT JOIN StockMovementEntity sm ON p.id = sm.productId " +
           "WHERE p.isActive = true " +
           "GROUP BY p " +
           "HAVING (p.initialQuantity + COALESCE(SUM(CASE WHEN sm.movementType = 'OUT' THEN -sm.quantity WHEN sm.movementType = 'IN' THEN sm.quantity ELSE 0 END), 0)) <= p.minimumStock")
    List<ProductEntity> findProductsWithCriticalStock();

    /**
     * Busca de forma paginada los productos cuyo nombre o código SKU contengan el
     * término indicado, ignorando mayúsculas/minúsculas. Spring Data genera la
     * implementación automáticamente a partir del nombre del método.
     */
    Page<ProductEntity> findByNameContainingIgnoreCaseOrSkuCodeContainingIgnoreCase(
            String name, String skuCode, Pageable pageable);
}
