package edu.pucmm.cs.inventory.application;

import java.math.BigDecimal;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import edu.pucmm.cs.inventory.infrastructure.web.dto.DashboardMetricsResponseDTO;
import edu.pucmm.cs.inventory.infrastructure.web.dto.ProductResponseDTO;

/**
 * Servicio de Aplicación que arma las métricas e indicadores del tablero de
 * control, reutilizando los servicios de productos, categorías y movimientos.
 * <p>
 * Al agregar los datos aquí, el dashboard se expone en un único endpoint que
 * cualquier usuario autenticado puede consultar, sin otorgarle acceso a las
 * listas detalladas (que siguen protegidas por sus permisos).
 */
@Service
public class DashboardService {

    // Tope de productos que se traen para sumar unidades y valor del inventario.
    // Suficiente para la escala del proyecto (misma cota que usaba el frontend).
    private static final int AGGREGATE_PAGE_SIZE = 1000;

    private final ProductService productService;
    private final CategoryService categoryService;
    private final StockMovementService stockMovementService;

    public DashboardService(ProductService productService, CategoryService categoryService,
            StockMovementService stockMovementService) {
        this.productService = productService;
        this.categoryService = categoryService;
        this.stockMovementService = stockMovementService;
    }

    public DashboardMetricsResponseDTO getMetrics() {
        Page<ProductResponseDTO> products = productService.getProducts(null, PageRequest.of(0, AGGREGATE_PAGE_SIZE));

        long totalUnits = products.getContent().stream()
                .mapToLong(p -> p.getStockActual() != null ? p.getStockActual() : 0L)
                .sum();

        BigDecimal totalValue = products.getContent().stream()
                .map(p -> {
                    BigDecimal price = p.getPrice() != null ? p.getPrice() : BigDecimal.ZERO;
                    long stock = p.getStockActual() != null ? p.getStockActual() : 0L;
                    return price.multiply(BigDecimal.valueOf(stock));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long totalMovements = stockMovementService.getMovements(null, PageRequest.of(0, 1)).getTotalElements();

        DashboardMetricsResponseDTO metrics = new DashboardMetricsResponseDTO();
        metrics.setTotalProducts(products.getTotalElements());
        metrics.setTotalUnits(totalUnits);
        metrics.setTotalValue(totalValue);
        metrics.setTotalCategories(categoryService.getCategories().size());
        metrics.setTotalMovements(totalMovements);
        metrics.setCriticalStockCount(productService.getCriticalStockAlerts().size());
        return metrics;
    }
}
