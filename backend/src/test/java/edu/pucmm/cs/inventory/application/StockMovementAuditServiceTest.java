package edu.pucmm.cs.inventory.application;

import edu.pucmm.cs.inventory.infrastructure.persistence.entity.StockMovementEntity;
import edu.pucmm.cs.inventory.infrastructure.web.dto.StockMovementAuditResponseDTO;
import jakarta.persistence.EntityManager;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.DefaultRevisionEntity;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.AuditQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class StockMovementAuditServiceTest {

    @Mock
    private EntityManager entityManager;

    @InjectMocks
    private StockMovementAuditService stockMovementAuditService;

    private AuditReader auditReader;
    private AuditQuery auditQuery;

    @BeforeEach
    void setUp() {
        auditReader = mock(AuditReader.class);
        auditQuery = mock(AuditQuery.class);
    }

    @Test
    void getAllStockMovementAuditHistory_ReturnsRevisions() {
        try (MockedStatic<AuditReaderFactory> mockedFactory = mockStatic(AuditReaderFactory.class)) {
            mockedFactory.when(() -> AuditReaderFactory.get(entityManager)).thenReturn(auditReader);

            when(auditReader.createQuery()).thenReturn(mock(org.hibernate.envers.query.AuditQueryCreator.class));
            when(auditReader.createQuery().forRevisionsOfEntity(any(), anyBoolean(), anyBoolean())).thenReturn(auditQuery);
            when(auditQuery.addOrder(any())).thenReturn(auditQuery);

            StockMovementEntity entity = new StockMovementEntity();
            entity.setId(UUID.randomUUID());
            entity.setNewQuantity(10);
            entity.setUsername("admin");

            DefaultRevisionEntity revisionEntity = new DefaultRevisionEntity();
            revisionEntity.setId(2);
            revisionEntity.setTimestamp(new Date().getTime());

            Object[] row = {entity, revisionEntity, RevisionType.MOD};
            when(auditQuery.getResultList()).thenReturn(Collections.singletonList(row));

            List<StockMovementAuditResponseDTO> result = stockMovementAuditService.getAllStockMovementAuditHistory();

            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals(10, result.get(0).getNewQuantity());
            assertEquals("admin", result.get(0).getUsername());
            assertEquals("UPDATED", result.get(0).getRevisionType());
            assertEquals(entity.getId(), result.get(0).getEntityId());
        }
    }
}
