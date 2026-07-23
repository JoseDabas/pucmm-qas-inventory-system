package edu.pucmm.cs.inventory.infrastructure.security;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Configuración de Seguridad Centralizada (DevSecOps).
 * <p>
 * Esta clase define la política de seguridad global de la aplicación,
 * configurándola
 * como un OAuth2 Resource Server. Se encarga de interceptar todas las
 * peticiones HTTP,
 * validar los tokens JWT emitidos por Keycloak y mapear los permisos
 * granulares.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    /**
     * Define la cadena de filtros de seguridad (Security Filter Chain).
     *
     * @param http Objeto HttpSecurity para configurar la seguridad web basada en
     *             HTTP.
     * @return La cadena de filtros configurada.
     * @throws Exception Si ocurre un error en la configuración.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // Habilitar CORS con la configuración por defecto o el bean proporcionado
                .cors(Customizer.withDefaults())

                // Deshabilitar CSRF (Cross-Site Request Forgery)
                // Dado que nuestra API es Stateless y utiliza tokens JWT en los encabezados
                // (Authorization: Bearer <token>)
                // en lugar de cookies de sesión, no es vulnerable a ataques CSRF, por lo que se
                // debe deshabilitar.
                .csrf(csrf -> csrf.disable())

                // Configuración de Sesiones a STATELESS
                // Garantiza que la aplicación no cree sesiones HTTP en el servidor para
                // almacenar el estado del usuario.
                // Cada petición debe ser autenticada independientemente con un JWT válido,
                // cumpliendo con los principios REST.
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // Autorización de Peticiones HTTP
                // Configura las reglas de acceso. Por defecto, exigimos que CUALQUIER petición
                // (anyRequest())
                // deba estar autenticada (authenticated()), implementando el principio de
                // "Seguridad por defecto".
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                        .requestMatchers("/actuator/**").permitAll()
                        // Permitir TRACE y QUERY explícitamente para que no devuelva 401, sino que
                        // llegue a Spring MVC y devuelva 405
                        .requestMatchers(org.springframework.http.HttpMethod.TRACE, "/**").permitAll()
                        .requestMatchers(org.springframework.http.HttpMethod.valueOf("QUERY"), "/**").permitAll()
                        .anyRequest().authenticated())

                // Configuración de cabeceras HTTP de seguridad. Esto incluye políticas como
                // Content Security Policy (CSP), X-Content-Type-Options, X-Frame-Options, etc.
                .headers(headers -> headers
                        .crossOriginResourcePolicy(corp -> corp.policy(
                                org.springframework.security.web.header.writers.CrossOriginResourcePolicyHeaderWriter.CrossOriginResourcePolicy.SAME_ORIGIN)))

                // Configuración del Servidor de Recursos OAuth2 (Resource Server)
                // Habilita a Spring Security para interceptar tokens JWT en el encabezado
                // Authorization.
                // Se inyecta un JwtAuthenticationConverter personalizado para extraer los roles
                // de Keycloak.
                .oauth2ResourceServer(oauth2 -> oauth2
                        .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));

        return http.build();
    }

    @org.springframework.beans.factory.annotation.Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri:http://keycloak:8080/realms/Inventario/protocol/openid-connect/certs}")
    private String jwkSetUri;

    /**
     * Decodificador de Tokens JWT personalizado para Entornos Heterogéneos (DevSecOps).
     * <p>
     * Obtiene dinámicamente las claves públicas de firmas RSA emitidas por el servidor de Keycloak 
     * a través de su endpoint JWK Set URI (JWKS). Realiza una validación criptográfica estricta 
     * de la firma del token y verifica las ventanas de vigencia temporal (claims 'exp' y 'nbf').
     * <p>
     * Esta implementación desacopla la validación estricta de la cadena literal de emisor (issuer), 
     * garantizando interoperabilidad entre peticiones internas de contenedores (Docker bridge) y 
     * peticiones externas del cliente (localhost / host.docker.internal).
     *
     * @return El bean {@link org.springframework.security.oauth2.jwt.JwtDecoder} configurado.
     */
    @Bean
    public org.springframework.security.oauth2.jwt.JwtDecoder jwtDecoder() {
        org.springframework.security.oauth2.jwt.NimbusJwtDecoder jwtDecoder = 
                org.springframework.security.oauth2.jwt.NimbusJwtDecoder.withJwkSetUri(jwkSetUri).build();
        jwtDecoder.setJwtValidator(new org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator<>(
                new org.springframework.security.oauth2.jwt.JwtTimestampValidator()
        ));
        return jwtDecoder;
    }

    /**
     * Configuración Global de Intercambio de Recursos entre Orígenes (CORS).
     * <p>
     * Define las reglas de acceso cruzado para permitir que la aplicación SPA (Single Page Application)
     * en Vite, así como los agentes de automatización E2E (Playwright) y pruebas de carga (k6) ejecutándose 
     * en contenedores Docker de CI/CD, puedan consumir la API de manera segura.
     *
     * @return El bean {@link CorsConfigurationSource} con la política de orígenes permitidos.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOriginPatterns(List.of("http://localhost:*", "http://host.docker.internal:*"));
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        configuration.setAllowedHeaders(
                Arrays.asList("Authorization", "Content-Type", "Accept", "traceparent", "X-Correlation-ID"));
        configuration.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }


    /**
     * Configuración del Firewall HTTP estricto de Spring Security.
     * Permite explícitamente el método TRACE para que la petición no sea bloqueada
     * prematuramente con un 400 Bad Request, permitiendo que Spring MVC la rechace
     * apropiadamente con un 405 Method Not Allowed, cumpliendo con el contrato de
     * la API.
     */
    @Bean
    public org.springframework.security.web.firewall.HttpFirewall allowTraceFirewall() {
        org.springframework.security.web.firewall.StrictHttpFirewall firewall = new org.springframework.security.web.firewall.StrictHttpFirewall();
        firewall.setAllowedHttpMethods(
                Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH", "TRACE", "QUERY"));
        return firewall;
    }

    /**
     * Conversor de Autenticación JWT personalizado.
     * <p>
     * Este bean es fundamental para la integración con Keycloak. Por defecto,
     * Spring Security busca
     * un campo 'scope' o 'scp' en el JWT. Sin embargo, Keycloak inyecta los roles
     * del Realm dentro
     * de la estructura JSON 'realm_access.roles'. Este conversor extrae esos roles
     * y los convierte
     * en objetos GrantedAuthority que Spring Security puede utilizar en anotaciones
     * como @PreAuthorize("hasAuthority('...')").
     *
     * @return El conversor JwtAuthenticationConverter configurado.
     */
    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();

        // Define el conversor encargado de extraer la colección de autoridades del
        // token JWT
        converter.setJwtGrantedAuthoritiesConverter(new KeycloakRealmRoleConverter());

        // Configura el nombre del claim que representa al usuario principal (subject)
        // en el token JWT. Keycloak utiliza 'preferred_username' para representar el
        // nombre de usuario.
        converter.setPrincipalClaimName("preferred_username");

        return converter;
    }

    /**
     * Clase interna que implementa la lógica específica para parsear el JWT de
     * Keycloak.
     * Extrae el array 'roles' anidado dentro del objeto 'realm_access' y añade el
     * prefijo "ROLE_"
     * a cada permiso granular (ej. "product:view" se convierte en
     * "ROLE_product:view").
     */
    private static class KeycloakRealmRoleConverter implements Converter<Jwt, Collection<GrantedAuthority>> {

        @Override
        public Collection<GrantedAuthority> convert(@NonNull Jwt jwt) {
            // Extrae la sección 'realm_access' del payload del token JWT
            Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");

            // Si el objeto es nulo o no contiene la clave 'roles', retorna una colección
            // vacía
            if (realmAccess == null || !realmAccess.containsKey("roles")) {
                return Collections.emptyList();
            }

            // Extrae la lista de roles definidos a nivel de Realm
            @SuppressWarnings("unchecked")
            List<String> roles = (List<String>) realmAccess.get("roles");

            // Convierte cada rol String en un SimpleGrantedAuthority agregando el prefijo
            // "ROLE_"
            // Esto es una convención estándar en Spring Security para el manejo de roles.
            return roles.stream()
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
        }
    }
}
