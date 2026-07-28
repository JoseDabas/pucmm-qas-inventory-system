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
 * Pruebas de contrato de API para los endpoints de auditoría.
 * Levanta el contexto completo (incluyendo base de datos vía Testcontainers)
 * en un puerto aleatorio y verifica usando RestAssured que la seguridad base perimetral 
 * se respete estrictamente, impidiendo filtraciones de datos sin autenticación.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuditApiContractTest {

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
     * Valida que la ruta de auditoría de productos exija obligatoriamente
     * autenticación, previniendo fuga de información confidencial.
     */
    @Test
    @DisplayName("GET contrato de auditoría de productos sin token devuelve 401 Unauthorized")
    void testGetAuditProductsWithoutAuthorizationReturns401() {
        RestAssured
            .given()
            .when()
                .get("/api/v1/audit/products")
            .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value());
    }

    /**
     * Verifica que el historial de movimientos de stock esté protegido
     * a nivel de API, retornando 401 ante accesos anónimos.
     */
    @Test
    @DisplayName("GET contrato de auditoría de movimientos sin token devuelve 401 Unauthorized")
    void testGetAuditStockMovementsWithoutAuthorizationReturns401() {
        RestAssured
            .given()
            .when()
                .get("/api/v1/audit/stock-movements")
            .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value());
    }
}
