package edu.pucmm.cs.inventory.infrastructure.security;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.security.oauth2.jwt.BadJwtException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import edu.pucmm.cs.inventory.application.ProductService;
import edu.pucmm.cs.inventory.infrastructure.web.ProductController;
import edu.pucmm.cs.inventory.infrastructure.web.dto.ProductResponseDTO;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Validación de JWT (Security Testing).
 * <p>
 * A diferencia de {@code ProductControllerApiTest}, que usa el post-processor
 * {@code jwt()} y omite la decodificación real, aquí se sustituye el
 * {@link JwtDecoder} con un mock para ejercitar el flujo completo del OAuth2
 * Resource Server: token ausente/malformado devuelve 401, y un token válido
 * es decodificado y sus roles de {@code realm_access.roles} se mapean a
 * autoridades vía {@code KeycloakRealmRoleConverter} en {@link SecurityConfig}.
 */
@WebMvcTest(ProductController.class)
@Import(SecurityConfig.class)
class JwtValidationApiTest {

    @Autowired
    private MockMvc mockMvc;

    // Se sustituye el JwtDecoder autoconfigurado (basado en issuer-uri) por un mock
    // para controlar la decodificación del token sin depender de Keycloak.
    @MockitoBean
    private JwtDecoder jwtDecoder;

    @MockitoBean
    private ProductService productService;

    // Construye un JWT válido con los roles indicados dentro de realm_access.roles.
    private Jwt jwtWithRoles(List<String> roles) {
        return Jwt.withTokenValue("valid-token")
                .header("alg", "RS256")
                .subject("test-user")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(300))
                .claim("realm_access", Map.of("roles", roles))
                .build();
    }

    // Sin encabezado Authorization -> 401 (autenticación requerida).
    @Test
    @DisplayName("Petición sin token JWT devuelve 401")
    void sinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isUnauthorized());
    }

    // Token malformado/inválido: el decoder lanza excepción -> 401.
    @Test
    @DisplayName("Token JWT malformado devuelve 401")
    void tokenMalformadoDevuelve401() throws Exception {
        when(jwtDecoder.decode(anyString())).thenThrow(new BadJwtException("Token inválido"));

        mockMvc.perform(get("/api/v1/products")
                .header("Authorization", "Bearer token.malformado"))
                .andExpect(status().isUnauthorized());
    }

    // Token válido con el rol requerido -> 200 (mapeo de realm_access.roles a authority).
    @Test
    @DisplayName("Token JWT válido con rol product:view devuelve 200")
    void tokenValidoConRolDevuelve200() throws Exception {
        when(jwtDecoder.decode(anyString())).thenReturn(jwtWithRoles(List.of("product:view")));
        when(productService.getProducts(any())).thenReturn(new PageImpl<>(List.of(new ProductResponseDTO())));

        mockMvc.perform(get("/api/v1/products")
                .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk());
    }

    // Token válido pero sin el rol requerido -> 403 (autenticado pero no autorizado).
    @Test
    @DisplayName("Token JWT válido sin el rol requerido devuelve 403")
    void tokenValidoSinRolDevuelve403() throws Exception {
        when(jwtDecoder.decode(anyString())).thenReturn(jwtWithRoles(List.of("stock:view")));

        mockMvc.perform(get("/api/v1/products")
                .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isForbidden());
    }
}
