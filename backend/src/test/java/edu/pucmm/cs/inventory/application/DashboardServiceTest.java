package edu.pucmm.cs.inventory.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import edu.pucmm.cs.inventory.infrastructure.web.dto.CategoryResponseDTO;
import edu.pucmm.cs.inventory.infrastructure.web.dto.DashboardMetricsResponseDTO;
import edu.pucmm.cs.inventory.infrastructure.web.dto.ProductResponseDTO;
import edu.pucmm.cs.inventory.infrastructure.web.dto.StockMovementResponseDTO;

/**
 * Pruebas unitarias de {@link DashboardService}: verifican que agrega
 * correctamente las métricas del tablero a partir de los servicios de dominio,
 * incluyendo el manejo de precios/stock nulos.
 */
@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock
    private ProductService productService;

    @Mock
    private CategoryService categoryService;

    @Mock
    private StockMovementService stockMovementService;

    @InjectMocks
    private DashboardService dashboardService;

    private ProductResponseDTO product(int stock, String price) {
        ProductResponseDTO p = new ProductResponseDTO();
        p.setStockActual(stock);
        p.setPrice(new BigDecimal(price));
        return p;
    }

    @Test
    @DisplayName("getMetrics agrega productos, unidades, valor, categorías, movimientos y críticos")
    void getMetricsAgregaLasMetricas() {
        ProductResponseDTO p1 = product(10, "2.00"); // 10 uds, valor 20.00
        ProductResponseDTO p2 = product(5, "3.00");  // 5 uds, valor 15.00
        ProductResponseDTO sinDatos = new ProductResponseDTO(); // stock/price null → 0

        when(productService.getProducts(isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(p1, p2, sinDatos), PageRequest.of(0, 1000), 3));
        when(categoryService.getCategories())
                .thenReturn(List.of(new CategoryResponseDTO(), new CategoryResponseDTO(), new CategoryResponseDTO()));
        when(stockMovementService.getMovements(isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<StockMovementResponseDTO>(Collections.emptyList(), PageRequest.of(0, 1), 7));
        when(productService.getCriticalStockAlerts()).thenReturn(List.of(p1));

        DashboardMetricsResponseDTO metrics = dashboardService.getMetrics();

        assertThat(metrics.getTotalProducts()).isEqualTo(3);
        assertThat(metrics.getTotalUnits()).isEqualTo(15);
        assertThat(metrics.getTotalValue()).isEqualByComparingTo("35.00");
        assertThat(metrics.getTotalCategories()).isEqualTo(3);
        assertThat(metrics.getTotalMovements()).isEqualTo(7);
        assertThat(metrics.getCriticalStockCount()).isEqualTo(1);
    }

    @Test
    @DisplayName("getMetrics sin productos devuelve ceros")
    void getMetricsSinProductos() {
        when(productService.getProducts(isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(Collections.emptyList(), PageRequest.of(0, 1000), 0));
        when(categoryService.getCategories()).thenReturn(Collections.emptyList());
        when(stockMovementService.getMovements(isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<StockMovementResponseDTO>(Collections.emptyList(), PageRequest.of(0, 1), 0));
        when(productService.getCriticalStockAlerts()).thenReturn(Collections.emptyList());

        DashboardMetricsResponseDTO metrics = dashboardService.getMetrics();

        assertThat(metrics.getTotalProducts()).isZero();
        assertThat(metrics.getTotalUnits()).isZero();
        assertThat(metrics.getTotalValue()).isEqualByComparingTo("0");
        assertThat(metrics.getTotalCategories()).isZero();
        assertThat(metrics.getTotalMovements()).isZero();
        assertThat(metrics.getCriticalStockCount()).isZero();
    }
}
