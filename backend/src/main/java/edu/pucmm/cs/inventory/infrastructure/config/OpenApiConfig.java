package edu.pucmm.cs.inventory.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración global de la documentación interactiva de la API con OpenAPI 3 (Swagger UI).
 * 
 * Centraliza la definición de los metadatos de la API y el esquema de seguridad global
 * para soportar la inyección y prueba de tokens JWT (Keycloak) directamente desde la interfaz de Swagger.
 */
@Configuration
public class OpenApiConfig {

    /**
     * Define y expone el bean personalizado de OpenAPI.
     * 
     * @return Configuración de OpenAPI con la información del proyecto y seguridad JWT Bearer.
     */
    @Bean
    public OpenAPI customOpenAPI() {
        // Nombre de referencia para el esquema de seguridad definido
        final String securitySchemeName = "bearerAuth";
        
        return new OpenAPI()
                // 1. Configuración de Metadatos de la API
                .info(new Info()
                        .title("API de Gestión de Inventarios")
                        .version("1.0.0")
                        .description("API REST central para la administración del sistema de inventarios empresariales. "
                                + "Desarrollado bajo Clean Architecture y Spring Boot 3. "
                                + "Provee capacidades de CRUD de productos e historial de movimientos auditables."))
                
                // 2. Aplicar el requerimiento de seguridad a todas las operaciones globalmente
                .addSecurityItem(new SecurityRequirement().addList(securitySchemeName))
                
                // 3. Declaración de Componentes (En este caso, el esquema HTTP Bearer para JWT)
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName, new SecurityScheme()
                                .name(securitySchemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Ingrese su token JWT provisto por Keycloak. No es necesario escribir 'Bearer ', Swagger lo hace automáticamente.")));
    }
}
