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

import edu.pucmm.cs.inventory.application.ProductAuditService;
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
 * 
 * A diferencia de los tests de controladores convencionales, aquí se sustituye el
 * JwtDecoder con un mock para ejercitar el flujo completo del OAuth2 Resource Server: 
 * comprueba que un token ausente o malformado devuelve 401, y que un token válido
 * se decodifica y sus roles se mapean correctamente a permisos de sistema.
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

    // El ProductController también depende de ProductAuditService; se mockea para
    // que el slice @WebMvcTest pueda instanciar el controlador.
    @MockitoBean
    private ProductAuditService productAuditService;

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

    /**
     * Asegura que cualquier petición a un recurso protegido que omita
     * la cabecera Authorization sea rechazada con estado HTTP 401.
     */
    @Test
    @DisplayName("Petición sin token JWT devuelve 401")
    void sinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/api/v1/products"))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Verifica que si el cliente provee un JWT firmado con una clave incorrecta,
     * expirado o con estructura inválida, el filtro de seguridad lo intercepte
     * devolviendo estado HTTP 401.
     */
    @Test
    @DisplayName("Token JWT malformado devuelve 401")
    void tokenMalformadoDevuelve401() throws Exception {
        when(jwtDecoder.decode(anyString())).thenThrow(new BadJwtException("Token inválido"));

        mockMvc.perform(get("/api/v1/products")
                .header("Authorization", "Bearer token.malformado"))
                .andExpect(status().isUnauthorized());
    }

    /**
     * Prueba el flujo de éxito: si el JWT es matemáticamente válido y contiene
     * el rol (claim) necesario para ejecutar la acción, la solicitud
     * debe procesarse exitosamente (HTTP 200).
     */
    @Test
    @DisplayName("Token JWT válido con rol product:view devuelve 200")
    void tokenValidoConRolDevuelve200() throws Exception {
        when(jwtDecoder.decode(anyString())).thenReturn(jwtWithRoles(List.of("product:view")));
        when(productService.getProducts(any(), any())).thenReturn(new PageImpl<>(List.of(new ProductResponseDTO())));

        mockMvc.perform(get("/api/v1/products")
                .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isOk());
    }

    /**
     * Comprueba la autorización (Authorization vs Authentication):
     * Un usuario puede tener una identidad válida en el sistema (token válido),
     * pero si no posee el rol granular requerido, debe recibir un estado HTTP 403.
     */
    @Test
    @DisplayName("Token JWT válido sin el rol requerido devuelve 403")
    void tokenValidoSinRolDevuelve403() throws Exception {
        when(jwtDecoder.decode(anyString())).thenReturn(jwtWithRoles(List.of("stock:view")));

        mockMvc.perform(get("/api/v1/products")
                .header("Authorization", "Bearer valid-token"))
                .andExpect(status().isForbidden());
    }
}
