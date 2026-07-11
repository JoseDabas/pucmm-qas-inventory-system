package edu.pucmm.cs.inventory.infrastructure.web.exception;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.TypeMismatchException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.persistence.EntityNotFoundException;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GlobalExceptionHandlerTest {

    private MockMvc mockMvc;

    // Controlador falso para forzar las excepciones dentro del contexto de Spring
    @RestController
    static class DummyController {
        @GetMapping("/readable") public void readable() { throw new HttpMessageNotReadableException("error", mock(HttpInputMessage.class)); }
        @GetMapping("/mismatch") public void mismatch() { throw new TypeMismatchException("val", Long.class); }
        @GetMapping("/illegal") public void illegal() { throw new IllegalArgumentException("arg"); }
        @GetMapping("/method-arg") 
        public void methodArg() throws Exception { 
            org.springframework.validation.BindingResult bindingResult = new org.springframework.validation.MapBindingResult(new java.util.HashMap<>(), "dummyObject");
            throw new MethodArgumentNotValidException(mock(org.springframework.core.MethodParameter.class), bindingResult); 
        }
        @GetMapping("/not-found") public void notFound() { throw new EntityNotFoundException("not found"); }
        @GetMapping("/denied") public void denied() { throw new AccessDeniedException("denied"); }
        @GetMapping("/integrity") public void integrity() { throw new DataIntegrityViolationException("integrity"); }
        @GetMapping("/method-not-supported") public void methodNotSupported() throws Exception { throw new HttpRequestMethodNotSupportedException("POST", List.of("GET")); }
        @GetMapping("/generic") public void generic() throws Exception { throw new Exception("generic"); }
    }

    @BeforeEach
    void setUp() {
        // Configuramos MockMvc para que intercepte los errores con el handler real
        mockMvc = MockMvcBuilders.standaloneSetup(new DummyController())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldHandleHttpMessageNotReadableException() throws Exception {
        mockMvc.perform(get("/readable")).andExpect(status().isBadRequest());
    }

    @Test
    void shouldHandleTypeMismatchException() throws Exception {
        mockMvc.perform(get("/mismatch")).andExpect(status().isBadRequest());
    }

    @Test
    void shouldHandleIllegalArgumentException() throws Exception {
        mockMvc.perform(get("/illegal")).andExpect(status().isBadRequest());
    }

    @Test
    void shouldHandleMethodArgumentNotValidException() {
        GlobalExceptionHandler handler = new GlobalExceptionHandler();
        // 4. MethodArgumentNotValidException (Mockeado al 100% para evitar NPE de Spring)
        org.springframework.web.bind.MethodArgumentNotValidException ex4 = mock(org.springframework.web.bind.MethodArgumentNotValidException.class);
        when(ex4.getMessage()).thenReturn("Validation failed");
        
        org.springframework.http.ProblemDetail pd4 = handler.handleMethodArgumentNotValidException(ex4);
        org.junit.jupiter.api.Assertions.assertEquals(org.springframework.http.HttpStatus.BAD_REQUEST.value(), pd4.getStatus());
    }

    @Test
    void shouldHandleEntityNotFoundException() throws Exception {
        mockMvc.perform(get("/not-found")).andExpect(status().isNotFound());
    }

    @Test
    void shouldHandleAccessDeniedException() throws Exception {
        mockMvc.perform(get("/denied")).andExpect(status().isForbidden());
    }

    @Test
    void shouldHandleDataIntegrityViolationException() throws Exception {
        mockMvc.perform(get("/integrity")).andExpect(status().isConflict());
    }

    @Test
    void shouldHandleHttpRequestMethodNotSupportedException() throws Exception {
        mockMvc.perform(get("/method-not-supported")).andExpect(status().isMethodNotAllowed());
    }

    @Test
    void shouldHandleGenericException() throws Exception {
        mockMvc.perform(get("/generic")).andExpect(status().isInternalServerError());
    }
}