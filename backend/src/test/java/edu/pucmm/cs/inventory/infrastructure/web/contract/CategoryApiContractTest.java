package edu.pucmm.cs.inventory.infrastructure.web.contract;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.HttpStatus;

/**
 * Pruebas de contrato de API para los endpoints de categorías.
 * Levanta el contexto completo (incluyendo base de datos vía Testcontainers)
 * en un puerto aleatorio y verifica usando RestAssured que la seguridad base perimetral 
 * se respete estrictamente, impidiendo filtraciones de datos sin autenticación.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CategoryApiContractTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @LocalServerPort
    private int port;

    @BeforeEach
    void setUp() {
        RestAssured.port = port;
    }

    /**
     * Asegura que el catálogo de categorías rechace peticiones 
     * no firmadas devolviendo el estado HTTP 401.
     */
    @Test
    @DisplayName("GET contrato de categorías sin token devuelve 401 Unauthorized")
    void testGetCategoriesWithoutAuthorizationReturns401() {
        RestAssured
            .given()
            .when()
                .get("/api/v1/categories")
            .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value());
    }
}
