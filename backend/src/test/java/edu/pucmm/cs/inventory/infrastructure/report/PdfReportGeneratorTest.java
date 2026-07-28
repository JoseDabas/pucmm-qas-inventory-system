package edu.pucmm.cs.inventory.infrastructure.report;

import edu.pucmm.cs.inventory.infrastructure.persistence.repository.MovementReportView;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas unitarias para el generador de reportes en PDF.
 * Asegura la correcta renderización del documento, incluyendo metadatos, 
 * tablas y el manejo correcto del formato binario (PDF magic numbers).
 */
class PdfReportGeneratorTest {

    /**
     * Verifica que si se provee un conjunto de datos válido, 
     * el generador produzca un arreglo de bytes que corresponda verdaderamente 
     * a un archivo PDF (comprobado a través del encabezado mágico "%PDF").
     */
    @Test
    @DisplayName("Genera un arreglo de bytes válido con cabecera PDF cuando existen datos")
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

    /**
     * Asegura que el reporte pueda generarse de manera exitosa incluso
     * cuando no se provee un nombre de categoría específico para el filtro,
     * comprobando la resistencia del layout a valores nulos.
     */
    @Test
    @DisplayName("Genera el reporte correctamente aunque la categoría sea nula")
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
