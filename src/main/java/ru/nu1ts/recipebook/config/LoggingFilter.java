package ru.nu1ts.recipebook.config;

import jakarta.annotation.Nonnull;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;
import org.springframework.web.util.ContentCachingResponseWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class LoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            @Nonnull HttpServletRequest request,
            @Nonnull HttpServletResponse response,
            @Nonnull FilterChain filterChain
    ) throws ServletException, IOException {

        ContentCachingRequestWrapper requestWrapper = new ContentCachingRequestWrapper(request, 10000);
        ContentCachingResponseWrapper responseWrapper = new ContentCachingResponseWrapper(response);

        Instant start = Instant.now();

        try {
            filterChain.doFilter(requestWrapper, responseWrapper);
        } finally {
            Instant finish = Instant.now();
            long durationMs = Duration.between(start, finish).toMillis();

            log.info("{} {} -> {} ({}ms)",
                    request.getMethod(),
                    request.getRequestURI(),
                    responseWrapper.getStatus(),
                    durationMs);

            if (responseWrapper.getStatus() >= 400) {
                log.warn("Error {} {} ({}ms). Body: {}",
                        request.getMethod(),
                        request.getRequestURI(),
                        durationMs,
                        getResponseBody(responseWrapper));
            }

            responseWrapper.copyBodyToResponse();
        }
    }

    private String getResponseBody(ContentCachingResponseWrapper responseWrapper) {
        byte[] buf = responseWrapper.getContentAsByteArray();
        if (buf.length > 0) {
            String body = new String(buf, StandardCharsets.UTF_8);
            return body.length() > 1000 ? body.substring(0, 1000) + "..." : body;
        }
        return "(empty)";
    }
}