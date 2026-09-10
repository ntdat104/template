package io.tcbs.template.filter;

import io.tcbs.template.constants.RequestKey;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;
import org.slf4j.MDC;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(1)
public class RequestIdFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
        throws ServletException, IOException {
        String requestId = request.getHeader(RequestKey.X_REQUEST_ID);

        if (!StringUtils.hasText(requestId)) {
            requestId = UUID.randomUUID().toString().replace("-", "");
        }

        try {
            // 1. Set for Logging/Tracing (MDC)
            MDC.put(RequestKey.X_REQUEST_ID, requestId);

            // 2. Set for Web Context Access (Attribute)
            request.setAttribute(RequestKey.X_REQUEST_ID, requestId);

            // Optionally, return in response header
            response.setHeader(RequestKey.X_REQUEST_ID, requestId);

            filterChain.doFilter(request, response);
        } finally {
            // CRUCIAL: Clean up MDC
            MDC.remove(RequestKey.X_REQUEST_ID);
        }
    }
}
