package edu.pucmm.cs.inventory.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.pucmm.cs.inventory.application.CategoryService;
import edu.pucmm.cs.inventory.infrastructure.security.SecurityConfig;
import edu.pucmm.cs.inventory.infrastructure.web.dto.CategoryRequestDTO;
import edu.pucmm.cs.inventory.infrastructure.web.dto.CategoryResponseDTO;
import edu.pucmm.cs.inventory.infrastructure.web.exception.CategoryInUseException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de API (slice @WebMvcTest) para {@link CategoryController}.
 * Verifica el enrutamiento HTTP, la validación de entrada y las reglas de
 * autorización (@PreAuthorize) sin cargar el contexto completo de la aplicación.
 */
@WebMvcTest(CategoryController.class)
@Import(SecurityConfig.class)
class CategoryControllerApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CategoryService categoryService;

    private CategoryRequestDTO validRequest;
    private CategoryResponseDTO sampleResponse;

    @BeforeEach
    void setUp() {
        validRequest = new CategoryRequestDTO();
        validRequest.setName("Electrónica");
        validRequest.setDescription("Dispositivos electrónicos");

        sampleResponse = new CategoryResponseDTO();
        sampleResponse.setId(UUID.randomUUID());
        sampleResponse.setName("Electrónica");
        sampleResponse.setProductCount(0);
    }

    private RequestPostProcessor jwtWith(String authority) {
        return jwt().authorities(new SimpleGrantedAuthority(authority));
    }

    // GET sin token -> 401
    @Test
    @DisplayName("GET categorías sin token devuelve 401")
    void getSinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isUnauthorized());
    }

    // GET con product:view -> 200
    @Test
    @DisplayName("GET categorías con product:view devuelve 200")
    void getConPermisoDevuelve200() throws Exception {
        when(categoryService.getCategories()).thenReturn(List.of(sampleResponse));
        mockMvc.perform(get("/api/v1/categories").with(jwtWith("product:view")))
                .andExpect(status().isOk());
    }

    // GET con permiso incorrecto -> 403
    @Test
    @DisplayName("GET categorías con permiso incorrecto devuelve 403")
    void getConPermisoIncorrectoDevuelve403() throws Exception {
        mockMvc.perform(get("/api/v1/categories").with(jwtWith("stock:view")))
                .andExpect(status().isForbidden());
    }

    // POST con product:manage -> 201
    @Test
    @DisplayName("POST crear categoría con product:manage devuelve 201")
    void postConPermisoDevuelve201() throws Exception {
        when(categoryService.createCategory(any())).thenReturn(sampleResponse);
        mockMvc.perform(post("/api/v1/categories")
                .with(jwtWith("product:manage"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated());
    }

    // POST con product:view (sin manage) -> 403
    @Test
    @DisplayName("POST crear categoría con product:view devuelve 403")
    void postConPermisoInsuficienteDevuelve403() throws Exception {
        mockMvc.perform(post("/api/v1/categories")
                .with(jwtWith("product:view"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isForbidden());
    }

    // POST con nombre vacío -> 400
    @Test
    @DisplayName("POST crear categoría con nombre vacío devuelve 400")
    void postConNombreVacioDevuelve400() throws Exception {
        validRequest.setName("");
        mockMvc.perform(post("/api/v1/categories")
                .with(jwtWith("product:manage"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest());
    }

    // POST con description explícitamente null -> 400 (Nulls.FAIL respeta el contrato)
    @Test
    @DisplayName("POST crear categoría con description null explícito devuelve 400")
    void postConDescriptionNullDevuelve400() throws Exception {
        mockMvc.perform(post("/api/v1/categories")
                .with(jwtWith("product:manage"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Electrónica\",\"description\":null}"))
                .andExpect(status().isBadRequest());
    }

    // POST sin description (campo opcional omitido) -> 201
    @Test
    @DisplayName("POST crear categoría sin description devuelve 201")
    void postSinDescriptionDevuelve201() throws Exception {
        when(categoryService.createCategory(any())).thenReturn(sampleResponse);
        mockMvc.perform(post("/api/v1/categories")
                .with(jwtWith("product:manage"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Electrónica\"}"))
                .andExpect(status().isCreated());
    }

    // DELETE con product:manage -> 204
    @Test
    @DisplayName("DELETE categoría con product:manage devuelve 204")
    void deleteConPermisoDevuelve204() throws Exception {
        mockMvc.perform(delete("/api/v1/categories/" + UUID.randomUUID())
                .with(jwtWith("product:manage")))
                .andExpect(status().isNoContent());
    }

    // DELETE con permiso incorrecto -> 403
    @Test
    @DisplayName("DELETE categoría con product:view devuelve 403")
    void deleteConPermisoIncorrectoDevuelve403() throws Exception {
        mockMvc.perform(delete("/api/v1/categories/" + UUID.randomUUID())
                .with(jwtWith("product:view")))
                .andExpect(status().isForbidden());
    }

    // DELETE de categoría con productos asociados -> 409
    @Test
    @DisplayName("DELETE categoría en uso devuelve 409")
    void deleteCategoriaEnUsoDevuelve409() throws Exception {
        doThrow(new CategoryInUseException("No se puede eliminar una categoría con productos asociados (2)."))
                .when(categoryService).deleteCategory(any());

        mockMvc.perform(delete("/api/v1/categories/" + UUID.randomUUID())
                .with(jwtWith("product:manage")))
                .andExpect(status().isConflict());
    }
}
