package edu.pucmm.cs.inventory.infrastructure.config;

import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.OAuth2Constants;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración del cliente de administración de Keycloak.
 * <p>
 * Expone un bean {@link Keycloak} autenticado con el grant client_credentials de
 * la service account del cliente 'inventory-client'. Lo consume
 * {@code KeycloakAdminService} para crear cuentas y asignar permisos (realm
 * roles). La conexión es perezosa: {@code KeycloakBuilder.build()} no contacta al
 * servidor hasta la primera llamada, por lo que el arranque no depende de que
 * Keycloak esté disponible.
 */
@Configuration
public class KeycloakAdminConfig {

    @Value("${keycloak.admin.server-url}")
    private String serverUrl;

    @Value("${keycloak.admin.realm}")
    private String realm;

    @Value("${keycloak.admin.client-id}")
    private String clientId;

    @Value("${keycloak.admin.client-secret}")
    private String clientSecret;

    @Bean
    public Keycloak keycloakAdminClient() {
        return KeycloakBuilder.builder()
                .serverUrl(serverUrl)
                .realm(realm)
                .grantType(OAuth2Constants.CLIENT_CREDENTIALS)
                .clientId(clientId)
                .clientSecret(clientSecret)
                .build();
    }
}
