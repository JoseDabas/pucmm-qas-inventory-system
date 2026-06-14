package edu.pucmm.cs.inventory.infrastructure.persistence.repository;

import edu.pucmm.cs.inventory.domain.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Interfaz de repositorio de Spring Data JPA para la entidad Category.
 *
 * Pertenece a la capa de infraestructura (Adaptadores de Persistencia).
 * Provee las operaciones CRUD sobre la tabla 'categories' y una consulta
 * derivada para localizar una categoría por su nombre (único).
 */
@Repository
public interface CategoryJpaRepository extends JpaRepository<Category, UUID> {

    /**
     * Busca una categoría por su nombre exacto.
     *
     * @param name nombre de la categoría.
     * @return la categoría si existe.
     */
    Optional<Category> findByName(String name);
}
