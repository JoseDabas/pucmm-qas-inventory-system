package edu.pucmm.cs.inventory.application;

import edu.pucmm.cs.inventory.infrastructure.persistence.entity.StockMovementEntity;
import edu.pucmm.cs.inventory.infrastructure.web.dto.StockMovementAuditResponseDTO;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import edu.pucmm.cs.inventory.infrastructure.persistence.entity.UserRevisionEntity;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.AuditEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Servicio de Aplicación para consultar el historial global de auditoría
 * de los movimientos de stock generado por Hibernate Envers.
 */
@Service
public class StockMovementAuditService {

    @PersistenceContext
    private EntityManager entityManager;

    /**
     * Devuelve el historial global de revisiones de todos los movimientos de stock,
     * de la más reciente a la más antigua.
     *
     * @return lista de revisiones de auditoría mapeadas a DTO.
     */
    @Transactional(readOnly = true)
    public List<StockMovementAuditResponseDTO> getAllStockMovementAuditHistory() {
        AuditReader auditReader = AuditReaderFactory.get(entityManager);

        @SuppressWarnings("unchecked")
        List<Object[]> revisions = auditReader.createQuery()
                .forRevisionsOfEntity(StockMovementEntity.class, false, true)
                .addOrder(AuditEntity.revisionNumber().desc())
                .getResultList();

        return revisions.stream()
                .map(this::mapRevision)
                .toList();
    }

    private StockMovementAuditResponseDTO mapRevision(Object[] row) {
        StockMovementEntity entity = (StockMovementEntity) row[0];
        UserRevisionEntity revisionInfo = (UserRevisionEntity) row[1];
        RevisionType revisionType = (RevisionType) row[2];

        StockMovementAuditResponseDTO dto = new StockMovementAuditResponseDTO();
        dto.setRevision(revisionInfo.getId());
        dto.setRevisionDate(OffsetDateTime.ofInstant(
                revisionInfo.getRevisionDate().toInstant(), ZoneId.systemDefault()));
        dto.setRevisionType(mapRevisionType(revisionType));
        dto.setModifiedBy(revisionInfo.getUsername());

        if (entity != null) {
            dto.setEntityId(entity.getId());
            dto.setProductId(entity.getProductId());
            dto.setMovementType(entity.getMovementType());
            dto.setPreviousQuantity(entity.getPreviousQuantity());
            dto.setNewQuantity(entity.getNewQuantity());
            if (entity.getDate() != null) {
                dto.setMovementDate(entity.getDate().atZone(ZoneId.systemDefault()).toOffsetDateTime());
            }
            dto.setUsername(entity.getUsername());
            dto.setObservations(entity.getObservations());
        }
        return dto;
    }

    private String mapRevisionType(RevisionType revisionType) {
        return switch (revisionType) {
            case ADD -> "CREATED";
            case MOD -> "UPDATED";
            case DEL -> "DELETED";
        };
    }
}
