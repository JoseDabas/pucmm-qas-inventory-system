package edu.pucmm.cs.inventory.application;

import edu.pucmm.cs.inventory.infrastructure.persistence.entity.ProductEntity;
import edu.pucmm.cs.inventory.infrastructure.persistence.repository.ProductJpaRepository;
import edu.pucmm.cs.inventory.infrastructure.web.dto.ProductAuditResponseDTO;
import jakarta.persistence.EntityManager;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.DefaultRevisionEntity;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.AuditQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.test.util.ReflectionTestUtils;

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

/**
 * Pruebas unitarias para ProductAuditService.
 * Verifica la correcta recuperación del historial de auditoría global de productos
 * utilizando las abstracciones de Hibernate Envers.
 */
@ExtendWith(MockitoExtension.class)
public class ProductAuditServiceGlobalTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private ProductJpaRepository productRepository;

    @InjectMocks
    private ProductAuditService productAuditService;

    private AuditReader auditReader;
    private AuditQuery auditQuery;

    @BeforeEach
    void setUp() {
        auditReader = mock(AuditReader.class);
        auditQuery = mock(AuditQuery.class);
        ReflectionTestUtils.setField(productAuditService, "entityManager", entityManager);
    }

    /**
     * Valida que el servicio consulte correctamente el motor de auditoría (Hibernate Envers)
     * y mapee las revisiones de entidades a los DTOs de auditoría esperados, 
     * incluyendo el tipo de revisión y el identificador de la entidad.
     */
    @Test
    @DisplayName("getAllProductAuditHistory devuelve la lista de revisiones mapeadas a DTO")
    void getAllProductAuditHistory_ReturnsRevisions() {
        try (MockedStatic<AuditReaderFactory> mockedFactory = mockStatic(AuditReaderFactory.class)) {
            mockedFactory.when(() -> AuditReaderFactory.get(entityManager)).thenReturn(auditReader);

            when(auditReader.createQuery()).thenReturn(mock(org.hibernate.envers.query.AuditQueryCreator.class));
            when(auditReader.createQuery().forRevisionsOfEntity(any(), anyBoolean(), anyBoolean())).thenReturn(auditQuery);
            when(auditQuery.addOrder(any())).thenReturn(auditQuery);

            ProductEntity entity = new ProductEntity();
            entity.setId(UUID.randomUUID());
            entity.setName("Test Product");

            DefaultRevisionEntity revisionEntity = new DefaultRevisionEntity();
            revisionEntity.setId(1);
            revisionEntity.setTimestamp(new Date().getTime());

            Object[] row = {entity, revisionEntity, RevisionType.ADD};
            when(auditQuery.getResultList()).thenReturn(Collections.singletonList(row));

            List<ProductAuditResponseDTO> result = productAuditService.getAllProductAuditHistory();

            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals("Test Product", result.get(0).getName());
            assertEquals("CREATED", result.get(0).getRevisionType());
            assertEquals(entity.getId(), result.get(0).getEntityId());
        }
    }
}
