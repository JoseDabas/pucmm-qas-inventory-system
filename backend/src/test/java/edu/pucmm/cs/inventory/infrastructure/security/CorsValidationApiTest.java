package edu.pucmm.cs.inventory.infrastructure.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import edu.pucmm.cs.inventory.application.ProductAuditService;
import edu.pucmm.cs.inventory.application.ProductService;
import edu.pucmm.cs.inventory.infrastructure.web.ProductController;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.options;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Validación de CORS (Security Testing).
 * <p>
 * Ejercita la configuración {@code corsConfigurationSource()} de
 * {@link SecurityConfig} mediante peticiones preflight (OPTIONS): un origen
 * permitido recibe las cabeceras CORS correctas, mientras que un origen no
 * autorizado es rechazado con 403.
 */
@WebMvcTest(ProductController.class)
@Import(SecurityConfig.class)
class CorsValidationApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    // El ProductController también depende de ProductAuditService; se mockea para
    // que el slice @WebMvcTest pueda instanciar el controlador.
    @MockitoBean
    private ProductAuditService productAuditService;

    private static final String ALLOWED_ORIGIN = "http://localhost:5173";

    // Preflight desde el origen permitido -> refleja el origen y permite credenciales.
    @Test
    @DisplayName("Preflight CORS desde origen permitido devuelve cabeceras CORS")
    void preflightOrigenPermitido() throws Exception {
        mockMvc.perform(options("/api/v1/products")
                .header("Origin", ALLOWED_ORIGIN)
                .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isOk())
                .andExpect(header().string("Access-Control-Allow-Origin", ALLOWED_ORIGIN))
                .andExpect(header().string("Access-Control-Allow-Credentials", "true"))
                .andExpect(header().string("Access-Control-Allow-Methods", containsString("POST")));
    }

    // Preflight desde un origen no permitido -> rechazado con 403.
    @Test
    @DisplayName("Preflight CORS desde origen NO permitido devuelve 403")
    void preflightOrigenNoPermitido() throws Exception {
        mockMvc.perform(options("/api/v1/products")
                .header("Origin", "http://evil.com")
                .header("Access-Control-Request-Method", "POST"))
                .andExpect(status().isForbidden());
    }
}
