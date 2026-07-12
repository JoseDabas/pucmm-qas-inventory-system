package edu.pucmm.cs.inventory.infrastructure.web.filter;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Filtro de Contexto de Telemetría (Capa de Infraestructura/Seguridad).
 * 
 * Intercepta todas las peticiones HTTP entrantes para extraer y propagar el contexto
 * de la solicitud (usuario, endpoint, ID de correlación) hacia el Mapped Diagnostic Context (MDC)
 * de SLF4J. Esto permite que los logs estructurados (JSON) incluyan esta información 
 * crucial para la observabilidad, auditoría y trazabilidad distribuida.
 * 
 * Este filtro actúa como puente para cumplir con los requerimientos de 
 * observabilidad.
 */
@Component
public class TelemetryContextFilter extends OncePerRequestFilter {

    private static final String CORRELATION_ID_HEADER = "X-Correlation-ID";
    private static final String MDC_CORRELATION_ID = "correlationId";
    private static final String MDC_USER = "user";
    private static final String MDC_ENDPOINT = "endpoint";

    /**
     * Lógica de filtrado interno que se ejecuta una vez por cada petición.
     * 
     * @param request La petición HTTP entrante.
     * @param response La respuesta HTTP saliente.
     * @param filterChain La cadena de filtros de Spring Security/Web.
     * @throws ServletException Si ocurre un error en el procesamiento del servlet.
     * @throws IOException Si ocurre un error de entrada/salida.
     */
    @Override
    protected void doFilterInternal(@org.springframework.lang.NonNull HttpServletRequest request, 
                                    @org.springframework.lang.NonNull HttpServletResponse response, 
                                    @org.springframework.lang.NonNull FilterChain filterChain)
            throws ServletException, IOException {
        
        try {
            // 1. User
            String user = "anonymous";
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated() && !auth.getPrincipal().equals("anonymousUser")) {
                user = auth.getName();
            }
            MDC.put(MDC_USER, user);

            // 2. Endpoint
            String endpoint = request.getMethod() + " " + request.getRequestURI();
            MDC.put(MDC_ENDPOINT, endpoint);

            // 3. Correlation ID
            String correlationId = request.getHeader(CORRELATION_ID_HEADER);
            if (correlationId == null || correlationId.isBlank()) {
                // Generar uno nuevo si no viene en el header
                correlationId = UUID.randomUUID().toString();
            }
            MDC.put(MDC_CORRELATION_ID, correlationId);

            filterChain.doFilter(request, response);
        } finally {
            // Limpiar el MDC para evitar fugas entre threads en el ThreadPool
            MDC.remove(MDC_USER);
            MDC.remove(MDC_ENDPOINT);
            MDC.remove(MDC_CORRELATION_ID);
            // trace_id y span_id son limpiados automáticamente por el OTel Java Agent
        }
    }
}
