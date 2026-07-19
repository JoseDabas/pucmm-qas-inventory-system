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

import edu.pucmm.cs.inventory.application.ProductAuditService;
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

// Anotación para configurar un test de Spring MVC enfocado en el ProductController
//WebMvcTest se utiliza para probar controladores específicos sin cargar todo el contexto de la aplicación, lo que hace que las pruebas sean más rápidas y aisladas. En este caso, se enfoca en ProductController.
@WebMvcTest(ProductController.class)
@Import(SecurityConfig.class)
class ProductControllerApiTest {

    // Inyección de dependencias para MockMvc y ObjectMapper
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    // MockitoBean se utiliza para crear un mock de ProductService que será
    // inyectado en el ProductController durante las pruebas.
    // Esto permite simular el comportamiento del servicio sin depender de su
    // implementación real.
    @MockitoBean
    private ProductService productService;

    // El ProductController también depende de ProductAuditService (endpoint de auditoría);
    // se mockea para que el slice @WebMvcTest pueda instanciar el controlador.
    @MockitoBean
    private ProductAuditService productAuditService;

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

    // Helper para simular un JWT con una autoridad especifica.
    private org.springframework.test.web.servlet.request.RequestPostProcessor jwtWith(String authority) {
        return jwt().authorities(new org.springframework.security.core.authority.SimpleGrantedAuthority(authority));
    }

    // GET sin token -> 401
    @Test
    @DisplayName("GET productos sin token devuelve 401")
    void getSinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isUnauthorized());
    }

    // GET con permiso correcto -> 200
    @Test
    @DisplayName("GET productos con product:view devuelve 200")
    void getConPermisoDevuelve200() throws Exception {
        when(productService.getProducts(any(), any())).thenReturn(new PageImpl<>(List.of(sampleResponse)));
        mockMvc.perform(get("/api/v1/products").with(jwtWith("product:view")))
                .andExpect(status().isOk());
    }

    // GET con parametro de busqueda -> 200
    @Test
    @DisplayName("GET productos con ?search filtra y devuelve 200")
    void getConBusquedaDevuelve200() throws Exception {
        when(productService.getProducts(any(), any())).thenReturn(new PageImpl<>(List.of(sampleResponse)));
        mockMvc.perform(get("/api/v1/products").param("search", "lap").with(jwtWith("product:view")))
                .andExpect(status().isOk());
    }

    // GET con permiso incorrecto -> 403
    @Test
    @DisplayName("GET productos con permiso incorrecto devuelve 403")
    void getConPermisoIncorrectoDevuelve403() throws Exception {
        mockMvc.perform(get("/api/v1/products").with(jwtWith("stock:view")))
                .andExpect(status().isForbidden());
    }

    // POST con product:manage -> 201
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

    // POST con viewer (sin product:manage) -> 403
    @Test
    @DisplayName("POST crear producto con product:view devuelve 403")
    void postConPermisoInsuficienteDevuelve403() throws Exception {
        mockMvc.perform(post("/api/v1/products")
                .with(jwtWith("product:view"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isForbidden());
    }

    // POST sin token -> 401
    @Test
    @DisplayName("POST crear producto sin token devuelve 401")
    void postSinTokenDevuelve401() throws Exception {
        mockMvc.perform(post("/api/v1/products")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isUnauthorized());
    }

    // POST con body invalido (nombre vacio) -> 400
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

    // POST con precio negativo -> 400
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

    // PUT actualizar con product:manage -> 200
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

    // DELETE con product:manage -> 204
    @Test
    @DisplayName("DELETE producto con product:manage devuelve 204")
    void deleteConPermisoDevuelve204() throws Exception {
        mockMvc.perform(delete("/api/v1/products/" + UUID.randomUUID())
                .with(jwtWith("product:manage")))
                .andExpect(status().isNoContent());
    }

    // GET alertas de stock critico con report:view -> 200
    @Test
    @DisplayName("GET alertas stock critico con report:view devuelve 200")
    void getAlertasConReportViewDevuelve200() throws Exception {
        when(productService.getCriticalStockAlerts()).thenReturn(List.of(sampleResponse));
        mockMvc.perform(get("/api/v1/products/alerts/critical-stock").with(jwtWith("report:view")))
                .andExpect(status().isOk());
    }

    // GET alertas de stock critico con permiso incorrecto -> 403
    @Test
    @DisplayName("GET alertas stock critico con product:view devuelve 403")
    void getAlertasConPermisoIncorrectoDevuelve403() throws Exception {
        mockMvc.perform(get("/api/v1/products/alerts/critical-stock").with(jwtWith("product:view")))
                .andExpect(status().isForbidden());
    }

    // GET alertas de stock critico sin token -> 401
    @Test
    @DisplayName("GET alertas stock critico sin token devuelve 401")
    void getAlertasSinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/api/v1/products/alerts/critical-stock"))
                .andExpect(status().isUnauthorized());
    }

}
