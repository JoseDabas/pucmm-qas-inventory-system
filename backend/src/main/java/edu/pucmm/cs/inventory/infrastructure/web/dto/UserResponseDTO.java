package edu.pucmm.cs.inventory.infrastructure.web.dto;

import java.util.Set;

import com.fasterxml.jackson.annotation.JsonInclude;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * DTO de salida que representa una cuenta de usuario para la sección de
 * administración: sus datos básicos, el rol del sistema resuelto (si sus
 * permisos coinciden con un rol del catálogo) y sus permisos efectivos.
 */
@Schema(description = "Representa una cuenta de usuario y sus permisos.")
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserResponseDTO {

    @Schema(description = "Identificador del usuario en Keycloak", example = "8f0c2e2a-1b3c-4d5e-6f70-8192a3b4c5d6")
    private String id;

    @Schema(description = "Nombre de usuario", example = "jperez")
    private String username;

    @Schema(description = "Correo electrónico", example = "jperez@inventario.local")
    private String email;

    @Schema(description = "Nombre(s)", example = "Juan")
    private String firstName;

    @Schema(description = "Apellido(s)", example = "Pérez")
    private String lastName;

    @Schema(description = "Indica si la cuenta está habilitada", example = "true")
    private boolean enabled;

    @Schema(description = "Nombre del rol del sistema resuelto a partir de los permisos; null si es una combinación personalizada", example = "VIEWER", nullable = true)
    private String role;

    @Schema(description = "Permisos granulares efectivos de la cuenta")
    private Set<String> permissions;

    // Getters
    public String getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public boolean isEnabled() { return enabled; }
    public String getRole() { return role; }
    public Set<String> getPermissions() { return permissions; }

    // Setters
    public void setId(String id) { this.id = id; }
    public void setUsername(String username) { this.username = username; }
    public void setEmail(String email) { this.email = email; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public void setRole(String role) { this.role = role; }
    public void setPermissions(Set<String> permissions) { this.permissions = permissions; }
}
