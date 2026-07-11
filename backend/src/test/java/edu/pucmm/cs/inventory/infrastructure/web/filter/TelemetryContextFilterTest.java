package edu.pucmm.cs.inventory.infrastructure.web.filter;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import jakarta.servlet.FilterChain;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TelemetryContextFilterTest {

    @AfterEach
    void tearDown() {
        // Limpiamos el contexto después de cada test para no contaminar
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldCoverAllBranchesForSonarQube() throws Exception {
        TelemetryContextFilter filter = new TelemetryContextFilter();
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain chain = (req, res) -> {}; // Dummy chain que no hace nada

        // RAMA 1: Sin Authentication, sin header correlation (auth == null, correlation == null)
        MockHttpServletRequest req1 = new MockHttpServletRequest();
        req1.setRequestURI("/api/test1");
        filter.doFilter(req1, response, chain);

        // RAMA 2: Con Auth, pero no autenticado (isAuthenticated == false) y correlationId en blanco
        Authentication auth2 = mock(Authentication.class);
        when(auth2.isAuthenticated()).thenReturn(false);
        SecurityContext ctx2 = mock(SecurityContext.class);
        when(ctx2.getAuthentication()).thenReturn(auth2);
        SecurityContextHolder.setContext(ctx2);
        
        MockHttpServletRequest req2 = new MockHttpServletRequest();
        req2.addHeader("X-Correlation-ID", "   "); // Fuerza la condición isBlank()
        filter.doFilter(req2, response, chain);

        // RAMA 3: Autenticado, pero es el usuario por defecto "anonymousUser"
        Authentication auth3 = mock(Authentication.class);
        when(auth3.isAuthenticated()).thenReturn(true);
        when(auth3.getPrincipal()).thenReturn("anonymousUser");
        SecurityContext ctx3 = mock(SecurityContext.class);
        when(ctx3.getAuthentication()).thenReturn(auth3);
        SecurityContextHolder.setContext(ctx3);
        
        MockHttpServletRequest req3 = new MockHttpServletRequest();
        req3.addHeader("X-Correlation-ID", "id-valido-123"); // Fuerza la rama de usar el ID existente
        filter.doFilter(req3, response, chain);

        // RAMA 4: Autenticado con usuario real
        Authentication auth4 = mock(Authentication.class);
        when(auth4.isAuthenticated()).thenReturn(true);
        when(auth4.getPrincipal()).thenReturn("usuarioReal");
        when(auth4.getName()).thenReturn("usuarioReal");
        SecurityContext ctx4 = mock(SecurityContext.class);
        when(ctx4.getAuthentication()).thenReturn(auth4);
        SecurityContextHolder.setContext(ctx4);
        
        MockHttpServletRequest req4 = new MockHttpServletRequest();
        filter.doFilter(req4, response, chain);
    }
}