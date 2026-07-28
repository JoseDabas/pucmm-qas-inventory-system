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
 * Pruebas de API (slice WebMvcTest) para CategoryController.
 * Verifica el enrutamiento HTTP, la validación de entrada y las reglas de
 * autorización (PreAuthorize) sin cargar el contexto completo de la aplicación.
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

    /**
     * Asegura que el catálogo de categorías no sea público, forzando 
     * a que el cliente provea un token JWT (HTTP 401).
     */
    @Test
    @DisplayName("GET categorías sin token devuelve 401")
    void getSinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/api/v1/categories"))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Comprueba que los usuarios con permiso de lectura de productos
     * también puedan listar el diccionario de categorías.
     */
    @Test
    @DisplayName("GET categorías con product:view devuelve 200")
    void getConPermisoDevuelve200() throws Exception {
        when(categoryService.getCategories()).thenReturn(List.of(sampleResponse));
        mockMvc.perform(get("/api/v1/categories").with(jwtWith("product:view")))
                .andExpect(status().isOk());
    }

    /**
     * Valida que roles cruzados (ej. visualizador de stock) no puedan 
     * inspeccionar la configuración estructural de las categorías (HTTP 403).
     */
    @Test
    @DisplayName("GET categorías con permiso incorrecto devuelve 403")
    void getConPermisoIncorrectoDevuelve403() throws Exception {
        mockMvc.perform(get("/api/v1/categories").with(jwtWith("stock:view")))
                .andExpect(status().isForbidden());
    }

    /**
     * Verifica la correcta inserción de una categoría cuando el usuario 
     * tiene autorización de administración de producto y envía un payload JSON válido.
     */
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

    /**
     * Previene la escalada de privilegios, asegurando que un usuario de solo lectura
     * no pueda inyectar nuevas categorías al sistema.
     */
    @Test
    @DisplayName("POST crear categoría con product:view devuelve 403")
    void postConPermisoInsuficienteDevuelve403() throws Exception {
        mockMvc.perform(post("/api/v1/categories")
                .with(jwtWith("product:view"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isForbidden());
    }

    /**
     * Valida las restricciones del esquema: el controlador debe rebotar
     * inmediatamente un payload cuyo nombre de categoría venga en blanco (HTTP 400).
     */
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

    /**
     * Asegura la robustez del binding JSON, rechazando inserciones si el cliente
     * envía la propiedad de descripción forzada a valor nulo, evitando NullPointerExceptions.
     */
    @Test
    @DisplayName("POST crear categoría con description null explícito devuelve 400")
    void postConDescriptionNullDevuelve400() throws Exception {
        mockMvc.perform(post("/api/v1/categories")
                .with(jwtWith("product:manage"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Electrónica\",\"description\":null}"))
                .andExpect(status().isBadRequest());
    }

    /**
     * Permite flexibilizar el cliente: si la descripción simplemente no se envía,
     * el sistema la asume vacía y crea la categoría correctamente.
     */
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

    /**
     * Prueba el flujo exitoso de eliminación de una categoría por su ID 
     * cuando el cliente tiene los privilegios adecuados (product:manage).
     */
    @Test
    @DisplayName("DELETE categoría con product:manage devuelve 204")
    void deleteConPermisoDevuelve204() throws Exception {
        mockMvc.perform(delete("/api/v1/categories/" + UUID.randomUUID())
                .with(jwtWith("product:manage")))
                .andExpect(status().isNoContent());
    }

    /**
     * Impide que usuarios sin autorización estructural puedan
     * purgar categorías del inventario.
     */
    @Test
    @DisplayName("DELETE categoría con product:view devuelve 403")
    void deleteConPermisoIncorrectoDevuelve403() throws Exception {
        mockMvc.perform(delete("/api/v1/categories/" + UUID.randomUUID())
                .with(jwtWith("product:view")))
                .andExpect(status().isForbidden());
    }

    /**
     * Maneja un escenario de negocio excepcional: rechaza (HTTP 409 Conflict) 
     * el intento de borrado de una categoría si esta aún tiene productos anidados, 
     * evitando orfandad de datos.
     */
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
