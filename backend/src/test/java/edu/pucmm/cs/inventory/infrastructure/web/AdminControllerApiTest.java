package edu.pucmm.cs.inventory.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;

import edu.pucmm.cs.inventory.application.KeycloakAdminService;
import edu.pucmm.cs.inventory.infrastructure.security.SecurityConfig;
import edu.pucmm.cs.inventory.infrastructure.security.SystemRole;
import edu.pucmm.cs.inventory.infrastructure.web.dto.CreateUserRequestDTO;
import edu.pucmm.cs.inventory.infrastructure.web.dto.UserResponseDTO;
import edu.pucmm.cs.inventory.infrastructure.web.exception.UsernameAlreadyExistsException;

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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Pruebas de API (slice @WebMvcTest) para {@link AdminController}.
 * Verifican el enrutamiento, la validación de entrada y que todas las
 * operaciones exijan el permiso 'user:manage'. El servicio que integra con
 * Keycloak se sustituye por un mock.
 */
@WebMvcTest(AdminController.class)
@Import(SecurityConfig.class)
class AdminControllerApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private KeycloakAdminService keycloakAdminService;

    private CreateUserRequestDTO validRequest;
    private UserResponseDTO sampleResponse;

    @BeforeEach
    void setUp() {
        validRequest = new CreateUserRequestDTO();
        validRequest.setUsername("jperez");
        validRequest.setEmail("jperez@inventario.local");
        validRequest.setFirstName("Juan");
        validRequest.setLastName("Pérez");
        validRequest.setPassword("Cambiar123!");
        validRequest.setRole(SystemRole.VIEWER);

        sampleResponse = new UserResponseDTO();
        sampleResponse.setId(UUID.randomUUID().toString());
        sampleResponse.setUsername("jperez");
        sampleResponse.setRole("VIEWER");
        sampleResponse.setPermissions(SystemRole.VIEWER.getPermissions());
    }

    private RequestPostProcessor jwtWith(String authority) {
        return jwt().authorities(new SimpleGrantedAuthority(authority));
    }

    // ---- GET /roles ----

    @Test
    @DisplayName("GET roles sin token devuelve 401")
    void getRolesSinTokenDevuelve401() throws Exception {
        mockMvc.perform(get("/api/v1/admin/roles"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET roles con user:manage devuelve 200 y el catálogo completo")
    void getRolesConPermisoDevuelve200() throws Exception {
        mockMvc.perform(get("/api/v1/admin/roles").with(jwtWith("user:manage")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(SystemRole.values().length));
    }

    @Test
    @DisplayName("GET roles con permiso incorrecto devuelve 403")
    void getRolesConPermisoIncorrectoDevuelve403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/roles").with(jwtWith("product:manage")))
                .andExpect(status().isForbidden());
    }

    // ---- GET /users ----

    @Test
    @DisplayName("GET usuarios con user:manage devuelve 200")
    void getUsersConPermisoDevuelve200() throws Exception {
        when(keycloakAdminService.listUsers()).thenReturn(List.of(sampleResponse));
        mockMvc.perform(get("/api/v1/admin/users").with(jwtWith("user:manage")))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("GET usuarios con permiso incorrecto devuelve 403")
    void getUsersConPermisoIncorrectoDevuelve403() throws Exception {
        mockMvc.perform(get("/api/v1/admin/users").with(jwtWith("product:view")))
                .andExpect(status().isForbidden());
    }

    // ---- POST /users ----

    @Test
    @DisplayName("POST crear cuenta con user:manage devuelve 201")
    void postCrearCuentaConPermisoDevuelve201() throws Exception {
        when(keycloakAdminService.createUser(any())).thenReturn(sampleResponse);
        mockMvc.perform(post("/api/v1/admin/users")
                .with(jwtWith("user:manage"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("POST crear cuenta con nombre de usuario duplicado devuelve 409")
    void postCrearCuentaDuplicadaDevuelve409() throws Exception {
        when(keycloakAdminService.createUser(any()))
                .thenThrow(new UsernameAlreadyExistsException("Ya existe una cuenta con ese nombre de usuario o correo."));
        mockMvc.perform(post("/api/v1/admin/users")
                .with(jwtWith("user:manage"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("POST crear cuenta con permiso insuficiente devuelve 403")
    void postCrearCuentaConPermisoInsuficienteDevuelve403() throws Exception {
        mockMvc.perform(post("/api/v1/admin/users")
                .with(jwtWith("product:manage"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("POST crear cuenta sin token devuelve 401")
    void postCrearCuentaSinTokenDevuelve401() throws Exception {
        mockMvc.perform(post("/api/v1/admin/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST crear cuenta con correo inválido devuelve 400")
    void postCrearCuentaConCorreoInvalidoDevuelve400() throws Exception {
        validRequest.setEmail("no-es-correo");
        mockMvc.perform(post("/api/v1/admin/users")
                .with(jwtWith("user:manage"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST crear cuenta con contraseña corta devuelve 400")
    void postCrearCuentaConPasswordCortaDevuelve400() throws Exception {
        validRequest.setPassword("123");
        mockMvc.perform(post("/api/v1/admin/users")
                .with(jwtWith("user:manage"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST crear cuenta sin rol devuelve 400")
    void postCrearCuentaSinRolDevuelve400() throws Exception {
        validRequest.setRole(null);
        mockMvc.perform(post("/api/v1/admin/users")
                .with(jwtWith("user:manage"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(validRequest)))
                .andExpect(status().isBadRequest());
    }

    // ---- PUT /users/{id}/role ----

    @Test
    @DisplayName("PUT cambiar rol con user:manage devuelve 200")
    void putCambiarRolConPermisoDevuelve200() throws Exception {
        String userId = UUID.randomUUID().toString();
        when(keycloakAdminService.changeUserRole(eq(userId), any())).thenReturn(sampleResponse);
        mockMvc.perform(put("/api/v1/admin/users/" + userId + "/role")
                .with(jwtWith("user:manage"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\":\"INVENTORY_MANAGER\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("PUT cambiar rol con permiso incorrecto devuelve 403")
    void putCambiarRolConPermisoIncorrectoDevuelve403() throws Exception {
        mockMvc.perform(put("/api/v1/admin/users/" + UUID.randomUUID() + "/role")
                .with(jwtWith("product:manage"))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"role\":\"INVENTORY_MANAGER\"}"))
                .andExpect(status().isForbidden());
    }
}
