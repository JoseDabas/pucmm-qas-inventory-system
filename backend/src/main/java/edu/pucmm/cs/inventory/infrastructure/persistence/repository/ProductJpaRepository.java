package edu.pucmm.cs.inventory.infrastructure.persistence.repository;

import edu.pucmm.cs.inventory.infrastructure.persistence.entity.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

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
    // Al ser una interfaz limpia, los métodos base de JpaRepository están disponibles implícitamente.
    // Futuras consultas personalizadas mediante "Query Methods" o @Query se ubicarían aquí.
}
