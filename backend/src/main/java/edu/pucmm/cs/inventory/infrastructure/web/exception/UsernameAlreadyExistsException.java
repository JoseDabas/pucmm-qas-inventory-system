package edu.pucmm.cs.inventory.infrastructure.web.exception;

/**
 * Excepción de negocio que indica que ya existe una cuenta con el mismo nombre
 * de usuario o correo en Keycloak. Se traduce a una respuesta HTTP 409 Conflict
 * en el GlobalExceptionHandler.
 */
public class UsernameAlreadyExistsException extends RuntimeException {

    public UsernameAlreadyExistsException(String message) {
        super(message);
    }
}
