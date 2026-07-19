package edu.pucmm.cs.inventory.application;

import edu.pucmm.cs.inventory.infrastructure.persistence.entity.ProductEntity;
import edu.pucmm.cs.inventory.infrastructure.web.dto.ProductAuditResponseDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityNotFoundException;
import jakarta.persistence.PersistenceContext;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.DefaultRevisionEntity;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.AuditEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Servicio de Aplicación (Caso de Uso) para consultar el historial de auditoría
 * de un
 * producto generado por Hibernate Envers.
 *
 * Lee las revisiones almacenadas en 'products_aud' + 'revinfo' mediante el
 * AuditReader,
 * exponiendo cada cambio (alta/modificación/baja) con la foto del producto en
 * ese momento.
 */
@Service
public class ProductAuditService {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Devuelve el historial de revisiones de un producto, de la más reciente a la
     * más antigua.
     *
     * @param productId identificador del producto.
     * @return lista de revisiones de auditoría mapeadas a DTO.
     * @throws EntityNotFoundException si el producto no tiene historial de
     *                                 auditoría.
     */
    @Transactional(readOnly = true)
    public List<ProductAuditResponseDTO> getProductAuditHistory(UUID productId) {
        AuditReader auditReader = AuditReaderFactory.get(entityManager);

        // forRevisionsOfEntity(clase, selectEntitiesOnly=false,
        // selectDeletedEntities=true):
        // cada fila es un Object[]{ entidad, revisión, tipoDeRevisión }, incluyendo las
        // bajas.
        @SuppressWarnings("unchecked")
        // Consulta las revisiones de la entidad ProductEntity filtrando por el ID del
        // producto
        List<Object[]> revisions = auditReader.createQuery()
                .forRevisionsOfEntity(ProductEntity.class, false, true)
                .add(AuditEntity.id().eq(productId))
                .addOrder(AuditEntity.revisionNumber().desc())
                .getResultList();

        if (revisions.isEmpty()) {
            throw new EntityNotFoundException(
                    "No existe historial de auditoría para el producto con ID: " + productId);
        }

        return revisions.stream()
                .map(this::mapRevision)
                .collect(Collectors.toList());
    }

    /**
     * Convierte una fila de revisión de Envers (entidad, metadatos de revisión,
     * tipo) en el DTO.
     */
    private ProductAuditResponseDTO mapRevision(Object[] row) {
        ProductEntity entity = (ProductEntity) row[0];
        DefaultRevisionEntity revisionInfo = (DefaultRevisionEntity) row[1];
        RevisionType revisionType = (RevisionType) row[2];

        ProductAuditResponseDTO dto = new ProductAuditResponseDTO();
        dto.setRevision(revisionInfo.getId());
        dto.setRevisionDate(LocalDateTime.ofInstant(
                revisionInfo.getRevisionDate().toInstant(), ZoneId.systemDefault()));
        dto.setRevisionType(mapRevisionType(revisionType));

        // En una baja (DEL) Envers solo rellena el id; el resto de campos quedan nulos.
        if (entity != null) {
            dto.setName(entity.getName());
            dto.setSkuCode(entity.getSkuCode());
            dto.setDescription(entity.getDescription());
            dto.setCategory(entity.getCategory() != null ? entity.getCategory().getName() : null);
            dto.setPrice(entity.getPrice());
            dto.setInitialQuantity(entity.getInitialQuantity());
            dto.setMinimumStock(entity.getMinimumStock());
            dto.setIsActive(entity.getIsActive());
        }
        return dto;
    }

    /**
     * Traduce el tipo de revisión de Envers a un nombre de dominio claro para el
     * cliente.
     */
    private String mapRevisionType(RevisionType revisionType) {
        return switch (revisionType) {
            case ADD -> "CREATED";
            case MOD -> "UPDATED";
            case DEL -> "DELETED";
        };
    }
}
