package edu.pucmm.cs.inventory.application;

import edu.pucmm.cs.inventory.domain.Category;
import edu.pucmm.cs.inventory.infrastructure.persistence.entity.ProductEntity;
import edu.pucmm.cs.inventory.infrastructure.persistence.entity.UserRevisionEntity;
import edu.pucmm.cs.inventory.infrastructure.web.dto.ProductAuditResponseDTO;
import jakarta.persistence.EntityManager;
import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;
import org.hibernate.envers.RevisionType;
import org.hibernate.envers.query.AuditEntity;
import org.hibernate.envers.query.AuditQuery;
import org.hibernate.envers.query.AuditQueryCreator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductAuditServiceTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private AuditReader auditReader;

    @Mock
    private AuditQueryCreator auditQueryCreator;

    @Mock
    private AuditQuery auditQuery;

    @InjectMocks
    private ProductAuditService productAuditService;

    private MockedStatic<AuditReaderFactory> mockedAuditReaderFactory;

    @BeforeEach
    void setUp() {
        mockedAuditReaderFactory = Mockito.mockStatic(AuditReaderFactory.class);
        mockedAuditReaderFactory.when(() -> AuditReaderFactory.get(entityManager)).thenReturn(auditReader);
    }

    @AfterEach
    void tearDown() {
        mockedAuditReaderFactory.close();
    }

    @Test
    void getAllProductAuditHistory_ShouldReturnMappedHistory() {
        // Arrange
        when(auditReader.createQuery()).thenReturn(auditQueryCreator);
        when(auditQueryCreator.forRevisionsOfEntity(eq(ProductEntity.class), eq(false), eq(true))).thenReturn(auditQuery);
        when(auditQuery.addOrder(any())).thenReturn(auditQuery);

        ProductEntity productEntity = new ProductEntity();
        productEntity.setId(UUID.randomUUID());
        productEntity.setName("Test Product");
        productEntity.setPrice(BigDecimal.TEN);
        
        Category category = new Category(UUID.randomUUID(), "Category1", null);
        productEntity.setCategory(category);

        UserRevisionEntity revisionEntity = new UserRevisionEntity();
        revisionEntity.setId(1);
        revisionEntity.setTimestamp(System.currentTimeMillis());
        revisionEntity.setUsername("testuser");

        Object[] revisionRow1 = new Object[]{productEntity, revisionEntity, RevisionType.ADD};
        Object[] revisionRow2 = new Object[]{null, revisionEntity, RevisionType.DEL}; // Simulate DEL where entity is null or partially filled

        when(auditQuery.getResultList()).thenReturn(List.of(revisionRow1, revisionRow2));

        // Act
        List<ProductAuditResponseDTO> result = productAuditService.getAllProductAuditHistory();

        // Assert
        assertNotNull(result);
        assertEquals(2, result.size());

        // Check ADD
        ProductAuditResponseDTO dto1 = result.get(0);
        assertEquals("CREATED", dto1.getRevisionType());
        assertEquals("testuser", dto1.getModifiedBy());
        assertEquals("Test Product", dto1.getName());
        assertEquals("Category1", dto1.getCategory());

        // Check DEL
        ProductAuditResponseDTO dto2 = result.get(1);
        assertEquals("DELETED", dto2.getRevisionType());
        assertNull(dto2.getName()); // Because entity is null
    }
}
