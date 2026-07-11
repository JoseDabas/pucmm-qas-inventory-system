package edu.pucmm.cs.inventory.infrastructure.web.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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
    void doFilterInternal_WithNullCorrelationId_GeneratesNewId() throws ServletException, IOException {
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
    void doFilterInternal_WithValidCorrelationId_UsesProvidedId() throws ServletException, IOException {
        String customId = "mi-id-personalizado";
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
    void doFilterInternal_WithAuthenticatedUser_UsesUsername() throws ServletException, IOException {
        when(request.getHeader("X-Correlation-ID")).thenReturn("custom-id");
        
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("testuser", "password")
        );

        doAnswer(invocation -> {
            String user = MDC.get("user");
            assertEquals("testuser", user, "User should match authenticated principal");
            return null;
        }).when(filterChain).doFilter(request, response);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        assertNull(MDC.get("user"));
    }
    
    @Test
    void doFilterInternal_WithAnonymousUser_UsesAnonymous() throws ServletException, IOException {
        when(request.getHeader("X-Correlation-ID")).thenReturn("custom-id");
        
        // Simulating anonymous user behavior in spring security
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken("anonymousUser", "password")
        );

        doAnswer(invocation -> {
            String user = MDC.get("user");
            assertEquals("anonymous", user, "User should be anonymous when principal is anonymousUser");
            return null;
        }).when(filterChain).doFilter(request, response);

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }
}
