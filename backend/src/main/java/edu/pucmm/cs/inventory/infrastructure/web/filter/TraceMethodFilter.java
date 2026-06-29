package edu.pucmm.cs.inventory.infrastructure.web.filter;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * Filtro de alta prioridad para interceptar peticiones TRACE y devolver 405 Method Not Allowed.
 * Esto es necesario porque por defecto Tomcat y Spring Security StrictHttpFirewall
 * devuelven 400 Bad Request, lo cual viola el estándar esperado por pruebas automatizadas
 * de seguridad y contratos de API como Schemathesis.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class TraceMethodFilter implements Filter {

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        if ("TRACE".equalsIgnoreCase(req.getMethod())) {
            HttpServletResponse res = (HttpServletResponse) response;
            res.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            return;
        }
        chain.doFilter(request, response);
    }
}
