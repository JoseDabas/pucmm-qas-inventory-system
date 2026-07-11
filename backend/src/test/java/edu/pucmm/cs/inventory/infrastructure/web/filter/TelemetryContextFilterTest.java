package edu.pucmm.cs.inventory.infrastructure.web.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TelemetryContextFilterTest {

    private TelemetryContextFilter filter;
    private HttpServletRequest request;
    private HttpServletResponse response;
    private FilterChain filterChain;

    @BeforeEach
    void setUp() {
        filter = new TelemetryContextFilter();
        request = mock(HttpServletRequest.class);
        response = mock(HttpServletResponse.class);
        filterChain = mock(FilterChain.class);
        
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/test");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        MDC.clear();
    }

    @Test
    void doFilterInternal_WithMissingCorrelationId_GeneratesNewId() throws ServletException, IOException {
        when(request.getHeader("X-Correlation-ID")).thenReturn(null);

        // We capture MDC in a custom answer when doFilter is called
        doAnswer(invocation -> {
            String correlationId = MDC.get("correlationId");
            assertNotNull(correlationId, "Correlation ID should not be null");
            assertFalse(correlationId.isBlank(), "Correlation ID should not be blank");
            
            String user = MDC.get("user");
            assertEquals("anonymous", user, "User should be anonymous");
            
            String endpoint = MDC.get("endpoint");
            assertEquals("GET /api/test", endpoint, "Endpoint should match request");
            return null;
        }).when(filterChain).doFilter(request, response);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        // Ensure MDC is cleaned up
        assertNull(MDC.get("correlationId"));
        assertNull(MDC.get("user"));
        assertNull(MDC.get("endpoint"));
    }
    
    @Test
    void doFilterInternal_WithBlankCorrelationId_GeneratesNewId() throws ServletException, IOException {
        when(request.getHeader("X-Correlation-ID")).thenReturn("   ");

        doAnswer(invocation -> {
            String correlationId = MDC.get("correlationId");
            assertNotNull(correlationId);
            assertFalse(correlationId.isBlank());
            assertNotEquals("   ", correlationId);
            return null;
        }).when(filterChain).doFilter(request, response);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void doFilterInternal_WithExistingCorrelationId_UsesExistingId() throws ServletException, IOException {
        String customId = "mi-id-123";
        when(request.getHeader("X-Correlation-ID")).thenReturn(customId);

        doAnswer(invocation -> {
            String correlationId = MDC.get("correlationId");
            assertEquals(customId, correlationId, "Should use provided Correlation ID");
            return null;
        }).when(filterChain).doFilter(request, response);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(MDC.get("correlationId"));
    }
    
    @Test
    void doFilterInternal_WithAuthenticatedUser_UsesUsername() throws Exception {
        // Arrange
        org.springframework.mock.web.MockHttpServletRequest mockReq = new org.springframework.mock.web.MockHttpServletRequest();
        org.springframework.mock.web.MockHttpServletResponse mockRes = new org.springframework.mock.web.MockHttpServletResponse();
        
        org.springframework.security.core.Authentication auth = mock(org.springframework.security.core.Authentication.class);
        when(auth.isAuthenticated()).thenReturn(true);
        when(auth.getPrincipal()).thenReturn("testUser");
        when(auth.getName()).thenReturn("testUser");
        
        org.springframework.security.core.context.SecurityContext context = mock(org.springframework.security.core.context.SecurityContext.class);
        when(context.getAuthentication()).thenReturn(auth);
        org.springframework.security.core.context.SecurityContextHolder.setContext(context);
        
        TelemetryContextFilter telemetryFilter = new TelemetryContextFilter();
        
        // Act: Capturamos el valor dentro del FilterChain, antes del 'finally'
        final String[] capturedUser = new String[1];
        jakarta.servlet.FilterChain customFilterChain = (req, res) -> {
            // NOTA: Ajustar la llave "user" si la constante MDC_USER del filtro es diferente (ej. "usuario")
            capturedUser[0] = org.slf4j.MDC.get("user"); 
        };
        
        telemetryFilter.doFilter(mockReq, mockRes, customFilterChain);
        
        // Assert
        assertEquals("testUser", capturedUser[0], "El usuario no se inyectó correctamente en el MDC");
        
        // Cleanup
        SecurityContextHolder.clearContext();
    }
    
    @Test
    void doFilterInternal_WithoutAuthentication_UsesAnonymous() throws ServletException, IOException {
        when(request.getHeader("X-Correlation-ID")).thenReturn("custom-id");
        
        // SecurityContext vacío (auth es null)
        SecurityContextHolder.clearContext();

        doAnswer(invocation -> {
            String user = MDC.get("user");
            assertEquals("anonymous", user, "User should be anonymous when authentication is null");
            return null;
        }).when(filterChain).doFilter(request, response);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }
}
