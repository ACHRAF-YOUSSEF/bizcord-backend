package com.bizcord.backend.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SecurityHeadersFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        // Prevent browsers from MIME-sniffing the content type
        response.setHeader("X-Content-Type-Options", "nosniff");

        // Deny framing entirely — prevents clickjacking
        response.setHeader("X-Frame-Options", "DENY");

        // Only allow HTTPS connections going forward (1 year, include subdomains)
        response.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");

        // Restrict referrer information sent to other origins
        response.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");

        // Disable browser features that are not needed by this API
        response.setHeader("Permissions-Policy",
                "camera=(), microphone=(), geolocation=(), payment=(), usb=()");

        // Content Security Policy — API-only backend: block all resource loading from this origin
        response.setHeader("Content-Security-Policy",
                "default-src 'none'; frame-ancestors 'none'");

        // Remove server fingerprinting header (set by some containers)
        response.setHeader("X-Powered-By", "");

        filterChain.doFilter(request, response);
    }
}
