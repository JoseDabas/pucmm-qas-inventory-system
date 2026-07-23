package edu.pucmm.cs.inventory.infrastructure.web.dto;

import edu.pucmm.cs.inventory.infrastructure.security.SystemRole;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO de entrada para que un administrador cree una cuenta de usuario y le
 * asigne un rol del sistema. El rol se traduce a un conjunto de permisos
 * (realm roles de Keycloak) al momento de crear la cuenta.
 */
@Schema(description = "Datos para crear una cuenta y asignarle un rol del sistema.")
public class CreateUserRequestDTO {

    @Schema(description = "Nombre de usuario único para iniciar sesión", example = "jperez", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "El nombre de usuario es obligatorio")
    @Size(max = 100, message = "El nombre de usuario no puede exceder los 100 caracteres")
    private String username;

    @Schema(description = "Correo electrónico del usuario", example = "jperez@inventario.local", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "El correo electrónico no es válido")
    private String email;

    @Schema(description = "Nombre(s) del usuario", example = "Juan")
    @Size(max = 100, message = "El nombre no puede exceder los 100 caracteres")
    private String firstName;

    @Schema(description = "Apellido(s) del usuario", example = "Pérez")
    @Size(max = 100, message = "El apellido no puede exceder los 100 caracteres")
    private String lastName;

    @Schema(description = "Contraseña inicial de la cuenta", example = "Cambiar123!", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, max = 100, message = "La contraseña debe tener entre 8 y 100 caracteres")
    private String password;

    @Schema(description = "Rol del sistema a asignar (define el conjunto de permisos)", example = "VIEWER", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "El rol es obligatorio")
    private SystemRole role;

    // Getters
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public String getFirstName() { return firstName; }
    public String getLastName() { return lastName; }
    public String getPassword() { return password; }
    public SystemRole getRole() { return role; }

    // Setters
    public void setUsername(String username) { this.username = username; }
    public void setEmail(String email) { this.email = email; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public void setPassword(String password) { this.password = password; }
    public void setRole(SystemRole role) { this.role = role; }
}
