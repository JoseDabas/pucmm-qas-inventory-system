package edu.pucmm.cs.inventory.application;

import edu.pucmm.cs.inventory.infrastructure.persistence.repository.MovementReportView;
import edu.pucmm.cs.inventory.infrastructure.persistence.repository.StockMovementJpaRepository;
import edu.pucmm.cs.inventory.infrastructure.persistence.repository.CategoryJpaRepository;
import edu.pucmm.cs.inventory.infrastructure.report.PdfReportGenerator;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Servicio de orquestación para la generación de reportes.
 * Aplica reglas de validación y coordina la extracción de datos con la generación del PDF.
 */
@Service
public class ReportService {

    private final StockMovementJpaRepository stockMovementRepository;
    private final CategoryJpaRepository categoryRepository;
    private final PdfReportGenerator pdfReportGenerator;

    public ReportService(StockMovementJpaRepository stockMovementRepository, CategoryJpaRepository categoryRepository, PdfReportGenerator pdfReportGenerator) {
        this.stockMovementRepository = stockMovementRepository;
        this.categoryRepository = categoryRepository;
        this.pdfReportGenerator = pdfReportGenerator;
    }

    /**
     * Extrae los movimientos en el rango dado (y opcionalmente por categoría)
     * y delega la creación del PDF.
     */
    public byte[] generateMovementReportPdf(LocalDateTime startDate, LocalDateTime endDate, UUID categoryId) {
        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Las fechas de inicio y fin son obligatorias.");
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("La fecha de inicio no puede ser posterior a la fecha de fin.");
        }

        List<MovementReportView> data = stockMovementRepository.findMovementReportData(startDate, endDate, categoryId);

        String categoryName = null;
        if (categoryId != null) {
            categoryName = categoryRepository.findById(categoryId)
                    .map(c -> c.getName())
                    .orElse("Categoría Desconocida");
        }

        return pdfReportGenerator.generateMovementReport(data, startDate, endDate, categoryName);
    }
}
