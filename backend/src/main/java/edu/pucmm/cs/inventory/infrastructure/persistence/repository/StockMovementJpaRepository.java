package edu.pucmm.cs.inventory.infrastructure.persistence.repository;

import edu.pucmm.cs.inventory.infrastructure.persistence.entity.StockMovementEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Interfaz de repositorio de Spring Data JPA para la entidad StockMovementEntity.
 * 
 * Abstrae el acceso a datos para el historial de movimientos, permitiendo ejecutar
 * operaciones sobre la tabla 'stock_movements' sin necesidad de escribir SQL directamente.
 */
@Repository // Registra el repositorio como un Bean de Spring en el contexto de aplicación
public interface StockMovementJpaRepository extends JpaRepository<StockMovementEntity, UUID> {
    // Se proporcionan capacidades CRUD completas de fábrica por Spring Data JPA.
}
