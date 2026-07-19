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
 * Pruebas de API (slice @WebMvcTest) para {@link StockMovementController}.
 * Verifica el enrutamiento HTTP, la validación de entrada y las reglas de
 * autorización (@PreAuthorize) sin cargar el contexto completo de la aplicación.
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
    void setUp() {
        validRequest = new StockMovementRequestDTO();
        validRequest.setProductId(UUID.randomUUID());
        validRequest.setMovementType(MovementType.IN);
        validRequest.setQuantity(10);
        validRequest.setObservations("Reposición");

        sampleResponse = new StockMovementResponseDTO();
        sampleResponse.setId(UUID.randomUUID());
        sampleResponse.setMovementType("IN");
        sampleResponse.setQuantity(10);
    }

    private RequestPostProcessor jwtWith(String authority) {
        return jwt().authorities(new SimpleGrantedAuthority(authority));
    }

    // GET sin token -> 401
    @Test
    @DisplayName("GET movimientos sin token devuelve 401")
    void getSinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/api/v1/stock-movements"))
                .andExpect(status().isUnauthorized());
    }

    // GET con report:view -> 200
    @Test
    @DisplayName("GET movimientos con report:view devuelve 200")
    void getConPermisoDevuelve200() throws Exception {
        when(stockMovementService.getMovements(any(), any())).thenReturn(new PageImpl<>(List.of(sampleResponse)));
        mockMvc.perform(get("/api/v1/stock-movements").with(jwtWith("report:view")))
                .andExpect(status().isOk());
    }

    // GET con permiso incorrecto -> 403
    @Test
    @DisplayName("GET movimientos con permiso incorrecto devuelve 403")
    void getConPermisoIncorrectoDevuelve403() throws Exception {
        mockMvc.perform(get("/api/v1/stock-movements").with(jwtWith("product:view")))
                .andExpect(status().isForbidden());
    }

    // POST con product:manage -> 201
    @Test
    @DisplayName("POST registrar movimiento con product:manage devuelve 201")
    void postConPermisoDevuelve201() throws Exception {
        when(stockMovementService.registerMovement(any())).thenReturn(sampleResponse);
        mockMvc.perform(post("/api/v1/stock-movements")
                .with(jwtWith("product:manage"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated());
    }

    // POST con report:view (sin manage) -> 403
    @Test
    @DisplayName("POST registrar movimiento con report:view devuelve 403")
    void postConPermisoInsuficienteDevuelve403() throws Exception {
        mockMvc.perform(post("/api/v1/stock-movements")
                .with(jwtWith("report:view"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isForbidden());
    }

    // POST sin token -> 401
    @Test
    @DisplayName("POST registrar movimiento sin token devuelve 401")
    void postSinTokenDevuelve401() throws Exception {
        mockMvc.perform(post("/api/v1/stock-movements")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isUnauthorized());
    }

    // POST con cantidad inválida (0) -> 400
    @Test
    @DisplayName("POST con cantidad inválida devuelve 400")
    void postConCantidadInvalidaDevuelve400() throws Exception {
        validRequest.setQuantity(0);
        mockMvc.perform(post("/api/v1/stock-movements")
                .with(jwtWith("product:manage"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest());
    }

    // POST cuya salida deja stock negativo (IllegalArgumentException) -> 400
    @Test
    @DisplayName("POST que deja stock negativo devuelve 400")
    void postStockNegativoDevuelve400() throws Exception {
        when(stockMovementService.registerMovement(any()))
                .thenThrow(new IllegalArgumentException("La salida solicitada supera el stock disponible."));
        validRequest.setMovementType(MovementType.OUT);
        mockMvc.perform(post("/api/v1/stock-movements")
                .with(jwtWith("product:manage"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest());
    }
}
