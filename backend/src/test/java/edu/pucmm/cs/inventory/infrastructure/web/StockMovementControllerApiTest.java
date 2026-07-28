package edu.pucmm.cs.inventory.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.pucmm.cs.inventory.application.StockMovementService;
import edu.pucmm.cs.inventory.domain.MovementType;
import edu.pucmm.cs.inventory.infrastructure.security.SecurityConfig;
import edu.pucmm.cs.inventory.infrastructure.web.dto.StockMovementRequestDTO;
import edu.pucmm.cs.inventory.infrastructure.web.dto.StockMovementResponseDTO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas de API (slice WebMvcTest) para StockMovementController.
 * Verifica el enrutamiento HTTP, la validación de entrada y las reglas de
 * autorización (PreAuthorize) sin cargar el contexto completo de la aplicación.
 */
@WebMvcTest(StockMovementController.class)
@Import(SecurityConfig.class)
class StockMovementControllerApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private StockMovementService stockMovementService;

    private StockMovementRequestDTO validRequest;
    private StockMovementResponseDTO sampleResponse;

    @BeforeEach
    @SuppressWarnings("unused")
    void setUp() {
        validRequest = new StockMovementRequestDTO();
        validRequest.setProductId(UUID.randomUUID());
        validRequest.setMovementType(MovementType.IN);
        validRequest.setQuantity(10);
        validRequest.setObservations("Reposición");

        sampleResponse = new StockMovementResponseDTO();
        sampleResponse.setId(UUID.randomUUID());
        sampleResponse.setMovementType("IN");
    }

    private RequestPostProcessor jwtWith(String authority) {
        return jwt().authorities(new SimpleGrantedAuthority(authority));
    }

    /**
     * Impide que usuarios anónimos consulten las bitácoras
     * de movimientos del almacén.
     */
    @Test
    @DisplayName("GET movimientos sin token devuelve 401")
    void getSinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/api/v1/stock-movements"))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Verifica que un rol autorizado (stock:view) obtenga una respuesta
     * paginada de todos los movimientos de mercancía (entradas y salidas).
     */
    @Test
    @DisplayName("GET movimientos con stock:view devuelve 200")
    void getConPermisoDevuelve200() throws Exception {
        when(stockMovementService.getMovements(any(), any())).thenReturn(new PageImpl<>(List.of(sampleResponse)));
        mockMvc.perform(get("/api/v1/stock-movements").with(jwtWith("stock:view")))
                .andExpect(status().isOk());
    }

    /**
     * Valida que permisos cruzados o insuficientes resulten
     * en un rechazo inmediato por parte de la configuración de seguridad.
     */
    @Test
    @DisplayName("GET movimientos con permiso incorrecto devuelve 403")
    void getConPermisoIncorrectoDevuelve403() throws Exception {
        mockMvc.perform(get("/api/v1/stock-movements").with(jwtWith("product:view")))
                .andExpect(status().isForbidden());
    }

    /**
     * Prueba el flujo de inserción de un movimiento físico válido (stock:manage),
     * garantizando su correcta deserialización y delegación al servicio de aplicación.
     */
    @Test
    @DisplayName("POST registrar movimiento con stock:manage devuelve 201")
    void postConPermisoDevuelve201() throws Exception {
        when(stockMovementService.registerMovement(any())).thenReturn(sampleResponse);
        mockMvc.perform(post("/api/v1/stock-movements")
                .with(jwtWith("stock:manage"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated());
    }

    /**
     * Verifica la robustez del binding en Jackson, asegurando que enviar
     * campos obligatorios como null explícito desencadene error 400 Bad Request.
     */
    @Test
    @DisplayName("POST movimiento con observations null explícito devuelve 400")
    void postConObservationsNullDevuelve400() throws Exception {
        String body = "{\"productId\":\"" + UUID.randomUUID()
                + "\",\"movementType\":\"IN\",\"quantity\":10,\"observations\":null}";
        mockMvc.perform(post("/api/v1/stock-movements")
                .with(jwtWith("stock:manage"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
                .andExpect(status().isBadRequest());
    }

    /**
     * Previene que usuarios de sólo lectura o con permisos menores intenten
     * afectar el inventario físico mediante inyección de payloads.
     */
    @Test
    @DisplayName("POST registrar movimiento con stock:view devuelve 403")
    void postConPermisoInsuficienteDevuelve403() throws Exception {
        mockMvc.perform(post("/api/v1/stock-movements")
                .with(jwtWith("stock:view"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isForbidden());
    }

    /**
     * Protege el endpoint de alteración de stock para que rechace
     * solicitudes no autenticadas antes de llegar a los interceptores de validación.
     */
    @Test
    @DisplayName("POST registrar movimiento sin token devuelve 401")
    void postSinTokenDevuelve401() throws Exception {
        mockMvc.perform(post("/api/v1/stock-movements")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Comprueba las aserciones de la capa de API (JSR-380): el movimiento
     * no puede registrar cero (0) unidades operadas.
     */
    @Test
    @DisplayName("POST con cantidad inválida devuelve 400")
    void postConCantidadInvalidaDevuelve400() throws Exception {
        validRequest.setQuantity(0);
        mockMvc.perform(post("/api/v1/stock-movements")
                .with(jwtWith("stock:manage"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest());
    }

    /**
     * Verifica la traducción de excepciones de negocio (ej. sobregiro de inventario)
     * a respuestas HTTP 400 mediante el GlobalExceptionHandler.
     */
    @Test
    @DisplayName("POST que deja stock negativo devuelve 400")
    void postStockNegativoDevuelve400() throws Exception {
        when(stockMovementService.registerMovement(any()))
                .thenThrow(new IllegalArgumentException("La salida solicitada supera el stock disponible."));
        validRequest.setMovementType(MovementType.OUT);
        mockMvc.perform(post("/api/v1/stock-movements")
                .with(jwtWith("stock:manage"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest());
    }
}
