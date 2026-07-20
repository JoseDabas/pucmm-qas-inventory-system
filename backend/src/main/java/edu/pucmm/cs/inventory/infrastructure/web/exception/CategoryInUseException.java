package edu.pucmm.cs.inventory.infrastructure.web.exception;

/**
 * Excepción de negocio que indica que una categoría no puede eliminarse porque
 * todavía tiene productos asociados. Se traduce a una respuesta HTTP 409 Conflict
 * con un mensaje descriptivo en el GlobalExceptionHandler.
 */
public class CategoryInUseException extends RuntimeException {

    public CategoryInUseException(String message) {
        super(message);
    }
}
