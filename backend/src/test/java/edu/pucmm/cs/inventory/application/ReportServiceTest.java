package edu.pucmm.cs.inventory.application;

import edu.pucmm.cs.inventory.domain.Category;
import edu.pucmm.cs.inventory.infrastructure.persistence.repository.CategoryJpaRepository;
import edu.pucmm.cs.inventory.infrastructure.persistence.repository.MovementReportView;
import edu.pucmm.cs.inventory.infrastructure.persistence.repository.StockMovementJpaRepository;
import edu.pucmm.cs.inventory.infrastructure.report.PdfReportGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @Mock
    private StockMovementJpaRepository stockMovementRepository;

    @Mock
    private CategoryJpaRepository categoryRepository;

    @Mock
    private PdfReportGenerator pdfReportGenerator;

    @InjectMocks
    private ReportService reportService;

    private LocalDateTime startDate;
    private LocalDateTime endDate;

    @BeforeEach
    void setUp() {
        startDate = LocalDateTime.now().minusDays(7);
        endDate = LocalDateTime.now();
    }

    @Test
    void testGenerateMovementReportPdf_NullDates_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> reportService.generateMovementReportPdf(null, endDate, null));
        assertThrows(IllegalArgumentException.class, () -> reportService.generateMovementReportPdf(startDate, null, null));
    }

    @Test
    void testGenerateMovementReportPdf_StartDateAfterEndDate_ThrowsException() {
        assertThrows(IllegalArgumentException.class, () -> reportService.generateMovementReportPdf(endDate, startDate, null));
    }

    @Test
    void testGenerateMovementReportPdf_WithValidDatesAndNoCategory_GeneratesPdf() {
        List<MovementReportView> mockData = new ArrayList<>();
        byte[] expectedPdfBytes = new byte[]{1, 2, 3};

        when(stockMovementRepository.findMovementReportData(startDate, endDate, null)).thenReturn(mockData);
        when(pdfReportGenerator.generateMovementReport(mockData, startDate, endDate, null)).thenReturn(expectedPdfBytes);

        byte[] result = reportService.generateMovementReportPdf(startDate, endDate, null);

        assertArrayEquals(expectedPdfBytes, result);
        verify(categoryRepository, never()).findById(any());
    }

    @Test
    void testGenerateMovementReportPdf_WithValidDatesAndCategory_GeneratesPdf() {
        UUID categoryId = UUID.randomUUID();
        List<MovementReportView> mockData = new ArrayList<>();
        byte[] expectedPdfBytes = new byte[]{1, 2, 3};
        Category category = new Category(categoryId, "Electronics", "Desc");

        when(stockMovementRepository.findMovementReportData(startDate, endDate, categoryId)).thenReturn(mockData);
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.of(category));
        when(pdfReportGenerator.generateMovementReport(mockData, startDate, endDate, "Electronics")).thenReturn(expectedPdfBytes);

        byte[] result = reportService.generateMovementReportPdf(startDate, endDate, categoryId);

        assertArrayEquals(expectedPdfBytes, result);
    }
    
    @Test
    void testGenerateMovementReportPdf_WithValidDatesAndUnknownCategory_GeneratesPdf() {
        UUID categoryId = UUID.randomUUID();
        List<MovementReportView> mockData = new ArrayList<>();
        byte[] expectedPdfBytes = new byte[]{1, 2, 3};

        when(stockMovementRepository.findMovementReportData(startDate, endDate, categoryId)).thenReturn(mockData);
        when(categoryRepository.findById(categoryId)).thenReturn(Optional.empty());
        when(pdfReportGenerator.generateMovementReport(mockData, startDate, endDate, "Categoría Desconocida")).thenReturn(expectedPdfBytes);

        byte[] result = reportService.generateMovementReportPdf(startDate, endDate, categoryId);

        assertArrayEquals(expectedPdfBytes, result);
    }
}
