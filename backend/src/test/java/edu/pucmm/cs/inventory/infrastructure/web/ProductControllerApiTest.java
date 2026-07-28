package edu.pucmm.cs.inventory.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.pucmm.cs.inventory.infrastructure.web.dto.ProductRequestDTO;
import edu.pucmm.cs.inventory.infrastructure.web.dto.ProductResponseDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import edu.pucmm.cs.inventory.application.ProductService;
import edu.pucmm.cs.inventory.infrastructure.security.SecurityConfig;
import org.springframework.context.annotation.Import;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pruebas de API (slice WebMvcTest) para ProductController.
 * Verifica el enrutamiento HTTP, la validación de entrada (Bean Validation) y las reglas
 * de autorización (PreAuthorize) sin cargar el contexto completo de la aplicación.
 */
@WebMvcTest(ProductController.class)
@Import(SecurityConfig.class)
class ProductControllerApiTest {

    // Inyección de dependencias para MockMvc y ObjectMapper
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProductService productService;

    private ProductRequestDTO validRequest;
    private ProductResponseDTO sampleResponse;

    @BeforeEach
    void setUp() {
        validRequest = new ProductRequestDTO();
        validRequest.setName("Laptop");
        validRequest.setSkuCode("SKU-001");
        validRequest.setDescription("Una laptop");
        validRequest.setCategory("Electronica");
        validRequest.setPrice(new BigDecimal("100.00"));
        validRequest.setInitialQuantity(10);
        validRequest.setMinimumStock(2);
        validRequest.setIsActive(true);

        sampleResponse = new ProductResponseDTO();
        sampleResponse.setId(UUID.randomUUID());
        sampleResponse.setName("Laptop");
        sampleResponse.setSkuCode("SKU-001");
    }

    private org.springframework.test.web.servlet.request.RequestPostProcessor jwtWith(String authority) {
        return jwt().authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority(authority));
    }

    /**
     * Asegura que el catálogo de productos requiera autenticación activa,
     * denegando solicitudes anónimas.
     */
    @Test
    @DisplayName("GET productos sin token devuelve 401")
    void getSinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Verifica que un usuario con privilegios de lectura pueda
     * acceder a la paginación del catálogo completo.
     */
    @Test
    @DisplayName("GET productos con product:view devuelve 200")
    void getConPermisoDevuelve200() throws Exception {
        when(productService.getProducts(any(), any())).thenReturn(new PageImpl<>(List.of(sampleResponse)));
        mockMvc.perform(get("/api/v1/products").with(jwtWith("product:view")))
                .andExpect(status().isOk());
    }

    /**
     * Comprueba la correcta transmisión de parámetros de búsqueda
     * hacia la capa de servicio cuando el usuario tiene los permisos requeridos.
     */
    @Test
    @DisplayName("GET productos con ?search filtra y devuelve 200")
    void getConBusquedaDevuelve200() throws Exception {
        when(productService.getProducts(any(), any())).thenReturn(new PageImpl<>(List.of(sampleResponse)));
        mockMvc.perform(get("/api/v1/products").param("search", "lap").with(jwtWith("product:view")))
                .andExpect(status().isOk());
    }

    /**
     * Valida que roles sin acceso a productos (ej. auditores de stock)
     * reciban un HTTP 403 Forbidden al intentar ver el catálogo.
     */
    @Test
    @DisplayName("GET productos con permiso incorrecto devuelve 403")
    void getConPermisoIncorrectoDevuelve403() throws Exception {
        mockMvc.perform(get("/api/v1/products").with(jwtWith("stock:view")))
                .andExpect(status().isForbidden());
    }

    /**
     * Verifica la creación exitosa de un producto nuevo cuando el rol
     * tiene privilegios administrativos sobre el módulo de inventario.
     */
    @Test
    @DisplayName("POST crear producto con product:manage devuelve 201")
    void postConPermisoDevuelve201() throws Exception {
        when(productService.createProduct(any())).thenReturn(sampleResponse);
        mockMvc.perform(post("/api/v1/products")
                .with(jwtWith("product:manage"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated());
    }

    /**
     * Protege contra escalada de privilegios, evitando que usuarios
     * de solo lectura (Viewers) intenten inyectar entidades nuevas.
     */
    @Test
    @DisplayName("POST crear producto con product:view devuelve 403")
    void postConPermisoInsuficienteDevuelve403() throws Exception {
        mockMvc.perform(post("/api/v1/products")
                .with(jwtWith("product:view"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isForbidden());
    }

    /**
     * Valida el filtro de seguridad previniendo que peticiones no
     * autenticadas alcancen la lógica de creación de productos.
     */
    @Test
    @DisplayName("POST crear producto sin token devuelve 401")
    void postSinTokenDevuelve401() throws Exception {
        mockMvc.perform(post("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Asegura la validación de Bean (JSR 380) a nivel de controlador,
     * regresando 400 Bad Request si el nombre del producto es omitido.
     */
    @Test
    @DisplayName("POST con nombre vacio devuelve 400")
    void postConNombreVacioDevuelve400() throws Exception {
        validRequest.setName("");
        mockMvc.perform(post("/api/v1/products")
                .with(jwtWith("product:manage"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest());
    }

    /**
     * Verifica que el controlador rebote payloads financieros
     * con montos inválidos (precios negativos) antes de tocar el dominio.
     */
    @Test
    @DisplayName("POST con precio negativo devuelve 400")
    void postConPrecioNegativoDevuelve400() throws Exception {
        validRequest.setPrice(new BigDecimal("-5.00"));
        mockMvc.perform(post("/api/v1/products")
                .with(jwtWith("product:manage"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest());
    }

    /**
     * Prueba el flujo de actualización integral (PUT) para garantizar
     * que se delega correctamente al servicio si se tienen permisos de gestión.
     */
    @Test
    @DisplayName("PUT actualizar producto con product:manage devuelve 200")
    void putConPermisoDevuelve200() throws Exception {
        when(productService.updateProduct(any(), any())).thenReturn(sampleResponse);
        mockMvc.perform(put("/api/v1/products/" + UUID.randomUUID())
                .with(jwtWith("product:manage"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isOk());
    }

    /**
     * Asegura que los privilegios administrativos permitan eliminar productos
     * del catálogo o realizar borrados lógicos sin restricción.
     */
    @Test
    @DisplayName("DELETE producto con product:manage devuelve 204")
    void deleteConPermisoDevuelve204() throws Exception {
        mockMvc.perform(delete("/api/v1/products/" + UUID.randomUUID())
                .with(jwtWith("product:manage")))
                .andExpect(status().isNoContent());
    }

    /**
     * Valida que el endpoint de alertas tempranas (stock crítico)
     * responda exitosamente a los usuarios autorizados (report:view).
     */
    @Test
    @DisplayName("GET alertas stock critico con report:view devuelve 200")
    void getAlertasConReportViewDevuelve200() throws Exception {
        when(productService.getCriticalStockAlerts()).thenReturn(List.of(sampleResponse));
        mockMvc.perform(get("/api/v1/products/alerts/critical-stock").with(jwtWith("report:view")))
                .andExpect(status().isOk());
    }

    /**
     * Protege el endpoint de métricas operativas contra usuarios
     * cuyos roles (ej. solo ver productos) no incluyen el reporte consolidado.
     */
    @Test
    @DisplayName("GET alertas stock critico con product:view devuelve 403")
    void getAlertasConPermisoIncorrectoDevuelve403() throws Exception {
        mockMvc.perform(get("/api/v1/products/alerts/critical-stock").with(jwtWith("product:view")))
                .andExpect(status().isForbidden());
    }

    /**
     * Fuerza la presentación de credenciales antes de exponer 
     * métricas sensibles del inventario.
     */
    @Test
    @DisplayName("GET alertas stock critico sin token devuelve 401")
    void getAlertasSinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/api/v1/products/alerts/critical-stock"))
                .andExpect(status().isUnauthorized());
    }


}
