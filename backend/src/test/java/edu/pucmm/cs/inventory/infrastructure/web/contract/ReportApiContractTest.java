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
 * Pruebas de contrato de API para los endpoints de reportes.
 * Levanta el contexto completo (incluyendo base de datos vía Testcontainers)
 * en un puerto aleatorio y verifica usando RestAssured que la seguridad base perimetral 
 * se respete estrictamente, impidiendo filtraciones de datos sin autenticación.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ReportApiContractTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @LocalServerPort
    private int port;

    @BeforeEach
    public void setUp() {
        RestAssured.port = port;
    }

    @Test
    @DisplayName("GET contrato de reportes sin token devuelve 401 Unauthorized")
    public void testGetReportsWithoutAuthorizationReturns401() {
        RestAssured
            .given()
            .when()
                .get("/api/v1/reports/movements")
            .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value());
    }
}
