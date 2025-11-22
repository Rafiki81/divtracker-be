package com.rafiki18.divtracker_be.config;

import java.io.IOException;

import org.springframework.core.annotation.Order;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;

@Component
@Order(1)
@Slf4j
public class RequestLoggingFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request, 
            @NonNull HttpServletResponse response, 
            @NonNull FilterChain filterChain)
            throws ServletException, IOException {
        
        String path = request.getRequestURI();
        
        // Solo log para webhooks de Finnhub
        if (path.contains("/webhooks/finnhub")) {
            String method = request.getMethod();
            String userAgent = request.getHeader("User-Agent");
            String secret = request.getHeader("X-Finnhub-Secret");
            String contentType = request.getContentType();
            
            log.info("=== WEBHOOK REQUEST START ===");
            log.info("Method: {}, Path: {}", method, path);
            log.info("User-Agent: {}", userAgent);
            log.info("Content-Type: {}", contentType);
            log.info("Secret present: {}, Secret value: [{}]", secret != null, secret);
            log.info("=== WEBHOOK REQUEST START ===");
            
            try {
                filterChain.doFilter(request, response);
                log.info("=== WEBHOOK RESPONSE: {} ===", response.getStatus());
            } catch (Exception e) {
                log.error("=== WEBHOOK EXCEPTION: {} ===", e.getMessage(), e);
                throw e;
            }
        } else {
            filterChain.doFilter(request, response);
        }
    }
}
