package io.github.pankajsinghnirmal.accessguard.shared.trace;

import io.github.pankajsinghnirmal.accessguard.shared.http.RequestHeaders;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

public final class CorrelationIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String correlationId = request.getHeader(RequestHeaders.CORRELATION_ID);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        request.setAttribute(RequestCorrelationIdContext.REQUEST_ATTRIBUTE_CORRELATION_ID, correlationId);

        try {
            MDC.put("correlationId", correlationId);
            response.setHeader(RequestHeaders.CORRELATION_ID, correlationId);
            filterChain.doFilter(request, response);
        } finally {
            MDC.remove("correlationId");
            request.removeAttribute(RequestCorrelationIdContext.REQUEST_ATTRIBUTE_CORRELATION_ID);
        }
    }
}