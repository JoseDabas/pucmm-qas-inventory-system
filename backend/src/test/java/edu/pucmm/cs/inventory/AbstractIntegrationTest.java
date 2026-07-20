package edu.pucmm.cs.inventory;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

/**
 * Clase base para tests de integración con Testcontainers.
 * Levanta un PostgreSQL real efímero y aplica las migraciones Flyway sobre él.
 *
 * Usa el patrón de "contenedor singleton": el contenedor se arranca una sola vez
 * en un bloque estático y vive durante toda la ejecución de la JVM de tests (lo
 * limpia Ryuk al finalizar). NO se usa @Testcontainers/@Container porque esos
 * detienen el contenedor al terminar cada clase, y al haber varias clases de
 * integración que comparten el contexto de Spring cacheado, la siguiente clase
 * reutilizaría un datasource apuntando a un contenedor ya detenido (ConnectException).
 */
@SpringBootTest
public abstract class AbstractIntegrationTest {

    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("inventory_test")
            .withUsername("test_user")
            .withPassword("test_password");

    static {
        postgres.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        // Flyway aplica TUS migraciones reales (V1-V4) sobre el contenedor.
        registry.add("spring.flyway.enabled", () -> "true");
    }
}