package edu.pucmm.cs.inventory.infrastructure.web;

import io.restassured.RestAssured;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;

@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class ProductApiContractTest {

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
    public void testGetProductsWithoutAuthorizationReturns401() {
        // Esta prueba valida que el contrato de seguridad base (perimetral) funciona
        // correctamente, impidiendo el acceso a recursos protegidos sin enviar el header Authorization.
        RestAssured
            .given()
            .when()
                .get("/api/v1/products")
            .then()
                .statusCode(HttpStatus.UNAUTHORIZED.value());
    }
}
