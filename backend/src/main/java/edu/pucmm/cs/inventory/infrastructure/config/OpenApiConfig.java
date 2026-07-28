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

    /**
     * Bean que intercepta la generación del OpenAPI y modifica sus componentes
     * para alinear la documentación con el comportamiento estricto del backend.
     */
    @Bean
    public org.springdoc.core.customizers.OpenApiCustomizer globalOpenApiCustomizer() {
        return openApi -> {
            // 1. Modificar esquemas para restringir generación de datos por Schemathesis
            if (openApi.getComponents() != null && openApi.getComponents().getSchemas() != null) {
                openApi.getComponents().getSchemas().values().forEach(schema -> {
                    // Si el esquema es un objeto (o tipo por defecto), prohibir campos extras
                    if (schema.getType() == null || "object".equals(schema.getType())) {
                        schema.setAdditionalProperties(Boolean.FALSE);
                    }
                    
                    // Restringir UUIDs para que Schemathesis no genere strings vacíos o caracteres inválidos
                    if ("string".equals(schema.getType()) && "uuid".equals(schema.getFormat())) {
                        schema.setPattern("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
                    }
                    
                    // Iterar sobre las propiedades para encontrar UUIDs anidados (ej. en DTOs)
                    if (schema.getProperties() != null) {
                        schema.getProperties().values().forEach(prop -> {
                            if (prop instanceof io.swagger.v3.oas.models.media.Schema) {
                                io.swagger.v3.oas.models.media.Schema propSchema = (io.swagger.v3.oas.models.media.Schema) prop;
                                if ("string".equals(propSchema.getType()) && "uuid".equals(propSchema.getFormat())) {
                                    propSchema.setPattern("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
                                }
                            }
                        });
                    }
                });
            }

            // 2. Agregar códigos de error estándar a todas las operaciones y restringir parámetros UUID
            if (openApi.getPaths() != null) {
                openApi.getPaths().values().forEach(pathItem -> {
                    pathItem.readOperations().forEach(operation -> {
                        // Restringir parámetros UUID para Schemathesis
                        if (operation.getParameters() != null) {
                            operation.getParameters().forEach(parameter -> {
                                if (parameter.getSchema() != null 
                                    && "string".equals(parameter.getSchema().getType()) 
                                    && "uuid".equals(parameter.getSchema().getFormat())) {
                                    parameter.getSchema().setPattern("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$");
                                }
                            });
                        }
                        
                        // Agregar códigos de error estándar
                        operation.getResponses()
                            .addApiResponse("400", new io.swagger.v3.oas.models.responses.ApiResponse().description("Bad Request: Petición inválida"))
                            .addApiResponse("401", new io.swagger.v3.oas.models.responses.ApiResponse().description("Unauthorized: Token JWT no proporcionado o inválido"))
                            .addApiResponse("403", new io.swagger.v3.oas.models.responses.ApiResponse().description("Forbidden: Permisos insuficientes"))
                            .addApiResponse("404", new io.swagger.v3.oas.models.responses.ApiResponse().description("Not Found: Recurso no encontrado"))
                            .addApiResponse("409", new io.swagger.v3.oas.models.responses.ApiResponse().description("Conflict: Violación de integridad de datos"))
                            .addApiResponse("500", new io.swagger.v3.oas.models.responses.ApiResponse().description("Internal Server Error"));
                    });
                });
            }
        };
    }
}
