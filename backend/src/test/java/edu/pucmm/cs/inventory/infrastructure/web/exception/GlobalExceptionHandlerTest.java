package edu.pucmm.cs.inventory.infrastructure.web.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;

import jakarta.persistence.EntityNotFoundException;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GlobalExceptionHandlerTest {

    private GlobalExceptionHandler handler;

    @BeforeEach
    void setUp() {
        handler = new GlobalExceptionHandler();
    }

    @Test
    void handleHttpMessageNotReadableException() {
        HttpMessageNotReadableException ex = mock(HttpMessageNotReadableException.class);
        when(ex.getMessage()).thenReturn("Test message");

        ProblemDetail pd = handler.handleHttpMessageNotReadableException(ex);

        assertEquals(HttpStatus.BAD_REQUEST.value(), pd.getStatus());
        assertEquals("Formato de petición inválido o tipo de dato incorrecto.", pd.getDetail());
    }

    @Test
    void handleTypeMismatchException() {
        org.springframework.beans.TypeMismatchException ex = mock(org.springframework.beans.TypeMismatchException.class);
        when(ex.getMessage()).thenReturn("Test message");

        ProblemDetail pd = handler.handleTypeMismatchException(ex);

        assertEquals(HttpStatus.BAD_REQUEST.value(), pd.getStatus());
        assertEquals("Formato de parámetro de URL inválido.", pd.getDetail());
    }

    @Test
    void handleIllegalArgumentException() {
        IllegalArgumentException ex = new IllegalArgumentException("Argument error");

        ProblemDetail pd = handler.handleIllegalArgumentException(ex);

        assertEquals(HttpStatus.BAD_REQUEST.value(), pd.getStatus());
        assertEquals("Argument error", pd.getDetail());
    }

    @Test
    void handleMethodArgumentNotValidException() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        when(ex.getMessage()).thenReturn("Validation error");

        ProblemDetail pd = handler.handleMethodArgumentNotValidException(ex);

        assertEquals(HttpStatus.BAD_REQUEST.value(), pd.getStatus());
        assertEquals("Datos de entrada no cumplen con las reglas de validación.", pd.getDetail());
    }

    @Test
    void handleEntityNotFoundException() {
        EntityNotFoundException ex = new EntityNotFoundException("Entity missing");

        ProblemDetail pd = handler.handleEntityNotFoundException(ex);

        assertEquals(HttpStatus.NOT_FOUND.value(), pd.getStatus());
        assertEquals("Entity missing", pd.getDetail());
    }

    @Test
    void handleAccessDeniedException() {
        AccessDeniedException ex = new AccessDeniedException("Denied");

        ProblemDetail pd = handler.handleAccessDeniedException(ex);

        assertEquals(HttpStatus.FORBIDDEN.value(), pd.getStatus());
        assertEquals("No tienes permisos suficientes para realizar esta acción.", pd.getDetail());
    }

    @Test
    void handleDataIntegrityViolationException() {
        org.springframework.dao.DataIntegrityViolationException ex = new org.springframework.dao.DataIntegrityViolationException("Conflict");

        ProblemDetail pd = handler.handleDataIntegrityViolationException(ex);

        assertEquals(HttpStatus.CONFLICT.value(), pd.getStatus());
        assertEquals("Violación de integridad de datos en la base de datos.", pd.getDetail());
    }

    @Test
    void handleHttpRequestMethodNotSupportedException() {
        org.springframework.web.HttpRequestMethodNotSupportedException ex = mock(org.springframework.web.HttpRequestMethodNotSupportedException.class);
        when(ex.getMessage()).thenReturn("Method not supported");
        when(ex.getSupportedHttpMethods()).thenReturn(Set.of(HttpMethod.GET, HttpMethod.POST));

        ResponseEntity<ProblemDetail> response = handler.handleHttpRequestMethodNotSupportedException(ex);

        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(HttpStatus.METHOD_NOT_ALLOWED.value(), response.getBody().getStatus());
        assertEquals("El método HTTP utilizado no está soportado para esta ruta.", response.getBody().getDetail());
        assertTrue(response.getHeaders().getAllow().contains(HttpMethod.GET));
        assertTrue(response.getHeaders().getAllow().contains(HttpMethod.POST));
    }

    @Test
    void handleHttpRequestMethodNotSupportedExceptionNullMethods() {
        org.springframework.web.HttpRequestMethodNotSupportedException ex = mock(org.springframework.web.HttpRequestMethodNotSupportedException.class);
        when(ex.getMessage()).thenReturn("Method not supported");
        when(ex.getSupportedHttpMethods()).thenReturn(null);

        ResponseEntity<ProblemDetail> response = handler.handleHttpRequestMethodNotSupportedException(ex);

        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(HttpStatus.METHOD_NOT_ALLOWED.value(), response.getBody().getStatus());
    }

    @Test
    void handleException() {
        Exception ex = new Exception("Internal error");

        ProblemDetail pd = handler.handleException(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), pd.getStatus());
        assertEquals("Ha ocurrido un error inesperado en el servidor.", pd.getDetail());
    }
}
