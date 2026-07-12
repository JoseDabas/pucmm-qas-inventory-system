package edu.pucmm.cs.inventory.infrastructure.web.exception;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import jakarta.persistence.EntityNotFoundException;

class GlobalExceptionHandlerTest {

    @Test
    void shouldCoverAllExceptionHandlers() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();

        // 1. HttpMessageNotReadableException
        HttpMessageNotReadableException ex1 = new HttpMessageNotReadableException("error",
                mock(HttpInputMessage.class));
        ProblemDetail pd1 = handler.handleHttpMessageNotReadableException(ex1);
        assertEquals(HttpStatus.BAD_REQUEST.value(), pd1.getStatus());

        // 2. TypeMismatchException
        TypeMismatchException ex2 = new TypeMismatchException("val", Long.class);
        ProblemDetail pd2 = handler.handleTypeMismatchException(ex2);
        assertEquals(HttpStatus.BAD_REQUEST.value(), pd2.getStatus());

        // 3. IllegalArgumentException
        ProblemDetail pd3 = handler.handleIllegalArgumentException(new IllegalArgumentException("arg"));
        assertEquals(HttpStatus.BAD_REQUEST.value(), pd3.getStatus());

        // 4. MethodArgumentNotValidException (Mockeado para evitar error de Spring)
        MethodArgumentNotValidException ex4 = mock(MethodArgumentNotValidException.class);
        when(ex4.getMessage()).thenReturn("Validation failed");
        ProblemDetail pd4 = handler.handleMethodArgumentNotValidException(ex4);
        assertEquals(HttpStatus.BAD_REQUEST.value(), pd4.getStatus());

        // 5. EntityNotFoundException
        ProblemDetail pd5 = handler.handleEntityNotFoundException(new EntityNotFoundException("not found"));
        assertEquals(HttpStatus.NOT_FOUND.value(), pd5.getStatus());

        // 6. AccessDeniedException
        ProblemDetail pd6 = handler.handleAccessDeniedException(new AccessDeniedException("denied"));
        assertEquals(HttpStatus.FORBIDDEN.value(), pd6.getStatus());

        // 7. DataIntegrityViolationException
        ProblemDetail pd7 = handler
                .handleDataIntegrityViolationException(new DataIntegrityViolationException("integrity"));
        assertEquals(HttpStatus.CONFLICT.value(), pd7.getStatus());

        // 8. HttpRequestMethodNotSupportedException (Rama null y Rama válida)
        HttpRequestMethodNotSupportedException ex8 = new HttpRequestMethodNotSupportedException("POST", List.of("GET"));
        var response = handler.handleHttpRequestMethodNotSupportedException(ex8);
        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, response.getStatusCode());

        HttpRequestMethodNotSupportedException ex8Null = mock(HttpRequestMethodNotSupportedException.class);
        when(ex8Null.getSupportedHttpMethods()).thenReturn(null);
        var responseNull = handler.handleHttpRequestMethodNotSupportedException(ex8Null);
        assertNotNull(responseNull);

        // 9. NoResourceFoundException / NoHandlerFoundException -> 404
        NoResourceFoundException ex9 = new NoResourceFoundException(HttpMethod.GET, "/ruta-inexistente");
        ProblemDetail pd9 = handler.handleNotFoundException(ex9);
        assertEquals(HttpStatus.NOT_FOUND.value(), pd9.getStatus());

        // 10. Exception general
        ProblemDetail pd10 = handler.handleException(new Exception("generic"));
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR.value(), pd10.getStatus());
    }
}