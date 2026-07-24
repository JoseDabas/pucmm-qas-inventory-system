package edu.pucmm.cs.inventory.infrastructure.report;

import edu.pucmm.cs.inventory.infrastructure.persistence.repository.MovementReportView;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class PdfReportGeneratorTest {

    @Test
    void testGenerateMovementReport_WithData_GeneratesPdfBytes() {
        PdfReportGenerator generator = new PdfReportGenerator();
        List<MovementReportView> data = new ArrayList<>();
        
        MovementReportView view1 = new MovementReportView() {
            @Override
            public String getProductName() { return "Laptop"; }
            @Override
            public String getCategoryName() { return "Electronics"; }
            @Override
            public String getMovementType() { return "IN"; }
            @Override
            public Integer getPreviousQuantity() { return 10; }
            @Override
            public Integer getNewQuantity() { return 15; }
            @Override
            public String getUsername() { return "admin"; }
            @Override
            public LocalDateTime getDate() { return LocalDateTime.now(); }
        };

        MovementReportView view2 = new MovementReportView() {
            @Override
            public String getProductName() { return "Mouse"; }
            @Override
            public String getCategoryName() { return "Electronics"; }
            @Override
            public String getMovementType() { return "OUT"; }
            @Override
            public Integer getPreviousQuantity() { return 15; }
            @Override
            public Integer getNewQuantity() { return 10; }
            @Override
            public String getUsername() { return "admin"; }
            @Override
            public LocalDateTime getDate() { return null; }
        };

        data.add(view1);
        data.add(view2);

        LocalDateTime startDate = LocalDateTime.now().minusDays(1);
        LocalDateTime endDate = LocalDateTime.now();

        byte[] pdfBytes = generator.generateMovementReport(data, startDate, endDate, "Electronics");

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
        // Verificar que el byte array comienza con el magic number de PDF '%PDF'
        assertEquals(0x25, pdfBytes[0]);
        assertEquals(0x50, pdfBytes[1]);
        assertEquals(0x44, pdfBytes[2]);
        assertEquals(0x46, pdfBytes[3]);
    }

    @Test
    void testGenerateMovementReport_WithoutCategory_GeneratesPdfBytes() {
        PdfReportGenerator generator = new PdfReportGenerator();
        List<MovementReportView> data = new ArrayList<>();

        LocalDateTime startDate = LocalDateTime.now().minusDays(1);
        LocalDateTime endDate = LocalDateTime.now();

        byte[] pdfBytes = generator.generateMovementReport(data, startDate, endDate, null);

        assertNotNull(pdfBytes);
        assertTrue(pdfBytes.length > 0);
    }
}
