package edu.pucmm.cs.inventory.infrastructure.report;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import edu.pucmm.cs.inventory.infrastructure.persistence.repository.MovementReportView;
import org.springframework.stereotype.Component;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Generador de PDFs usando OpenPDF.
 * Componente de infraestructura (no contiene lógica de negocio, solo formato visual).
 */
@Component
public class PdfReportGenerator {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public byte[] generateMovementReport(List<MovementReportView> movements, LocalDateTime startDate, LocalDateTime endDate, String categoryName) {
        Document document = new Document(PageSize.A4.rotate()); // Formato horizontal para tablas
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        try {
            PdfWriter.getInstance(document, out);
            document.open();

            // Título
            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Paragraph title = new Paragraph("Reporte de Movimientos de Inventario", titleFont);
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(10);
            document.add(title);

            // Metadatos / Filtros
            Font metaFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
            Paragraph metadata = new Paragraph();
            metadata.add(new Phrase("Generado el: " + LocalDateTime.now(java.time.ZoneId.systemDefault()).format(DATE_FORMATTER) + "\n", metaFont));
            metadata.add(new Phrase("Periodo: " + startDate.format(DATE_FORMATTER) + " a " + endDate.format(DATE_FORMATTER) + "\n", metaFont));
            metadata.add(new Phrase("Categoría filtrada: " + (categoryName != null ? categoryName : "Todas") + "\n", metaFont));
            metadata.setSpacingAfter(20);
            document.add(metadata);

            // Tabla
            PdfPTable table = new PdfPTable(7);
            table.setWidthPercentage(100);
            table.setWidths(new float[]{1.5f, 3f, 2f, 1f, 1f, 1f, 2f}); // Tamaños relativos

            // Cabeceras de tabla
            addTableHeader(table, "Fecha");
            addTableHeader(table, "Producto");
            addTableHeader(table, "Categoría");
            addTableHeader(table, "Tipo");
            addTableHeader(table, "Stock Ant.");
            addTableHeader(table, "Stock Nvo.");
            addTableHeader(table, "Usuario");

            // Filas de datos
            Font dataFont = FontFactory.getFont(FontFactory.HELVETICA, 9);
            for (MovementReportView m : movements) {
                addCell(table, m.getDate() != null ? m.getDate().format(DATE_FORMATTER) : "", dataFont);
                addCell(table, m.getProductName(), dataFont);
                addCell(table, m.getCategoryName(), dataFont);
                
                PdfPCell typeCell = new PdfPCell(new Phrase(m.getMovementType(), dataFont));
                typeCell.setPadding(5);
                // Colorear ligeramente IN o OUT
                if ("IN".equalsIgnoreCase(m.getMovementType())) {
                    typeCell.setBackgroundColor(new Color(220, 255, 220));
                } else if ("OUT".equalsIgnoreCase(m.getMovementType())) {
                    typeCell.setBackgroundColor(new Color(255, 220, 220));
                }
                table.addCell(typeCell);
                
                addCell(table, String.valueOf(m.getPreviousQuantity()), dataFont);
                addCell(table, String.valueOf(m.getNewQuantity()), dataFont);
                addCell(table, m.getUsername(), dataFont);
            }

            document.add(table);
            document.close();
            
        } catch (com.lowagie.text.DocumentException e) {
            throw new RuntimeException("Error de formato al generar el PDF del reporte", e);
        } catch (Exception e) {
            throw new RuntimeException("Error inesperado al generar el PDF", e);
        }

        return out.toByteArray();
    }

    private void addTableHeader(PdfPTable table, String headerTitle) {
        Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, Color.WHITE);
        PdfPCell header = new PdfPCell();
        header.setBackgroundColor(new Color(64, 64, 64));
        header.setPadding(6);
        header.setPhrase(new Phrase(headerTitle, headerFont));
        table.addCell(header);
    }
    
    private void addCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text != null ? text : "", font));
        cell.setPadding(5);
        table.addCell(cell);
    }
}
