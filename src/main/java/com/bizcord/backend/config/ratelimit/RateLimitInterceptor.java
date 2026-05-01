package com.bizcord.backend.config.ratelimit;

import com.bizcord.backend.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.HandlerMapping;

import java.time.Duration;
import java.util.Arrays;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${app.security.trusted-proxy-ips:}")
    private String trustedProxyIpsRaw;

    private Set<String> trustedProxyIps;

    @PostConstruct
    void init() {
        trustedProxyIps = Arrays.stream(trustedProxyIpsRaw.split("[,\\s]+"))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public boolean preHandle(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull Object handler
    ) throws Exception {
        if (!(handler instanceof HandlerMethod handlerMethod)) {
            return true;
        }

        RateLimit rateLimit = handlerMethod.getMethodAnnotation(RateLimit.class);
        if (rateLimit == null) {
            return true;
        }

        String key = resolveKey(request, rateLimit);
        Bucket bucket = buckets.computeIfAbsent(key, k -> createBucket(rateLimit));

        if (bucket.tryConsume(1)) {
            long availableTokens = bucket.getAvailableTokens();
            response.setHeader("X-Rate-Limit-Remaining", String.valueOf(availableTokens));
            return true;
        }

        response.setStatus(429);
        response.setContentType("application/json");
        response.setHeader("Retry-After", String.valueOf(rateLimit.durationMinutes() * 60));
        response.setHeader("X-Rate-Limit-Remaining", "0");

        objectMapper.writeValue(response.getWriter(),
                Map.of("error", "Rate limit exceeded. Try again later."));

        return false;
    }

    private Bucket createBucket(RateLimit rateLimit) {
        Bandwidth bandwidth = Bandwidth
                .builder()
                .capacity(rateLimit.limit())
                .refillGreedy(rateLimit.limit(), Duration.ofMinutes(rateLimit.durationMinutes()))
                .build();

        return Bucket.builder()
                .addLimit(bandwidth)
                .build();
    }

    private String resolveKey(HttpServletRequest request, RateLimit rateLimit) {
        Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        String endpoint = request.getMethod() + ":" + (pattern != null ? pattern.toString() : request.getRequestURI());

        RateLimitKeyType keyType = rateLimit.keyType();
        if (keyType == RateLimitKeyType.UID) return endpoint + ":uid:" + resolveUserId();
        if (keyType == RateLimitKeyType.IP_AND_UID) return endpoint + ":ip:" + resolveIp(request) + ":uid:" + resolveUserId();
        return endpoint + ":ip:" + resolveIp(request);
    }

    private String resolveIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();
        if (!trustedProxyIps.isEmpty() && trustedProxyIps.contains(remoteAddr)) {
            String xff = request.getHeader("X-Forwarded-For");
            if (xff != null && !xff.isBlank()) {
                String[] parts = xff.split(",");
                return parts[parts.length - 1].trim();
            }
        }
        return remoteAddr;
    }

    private String resolveUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User user) {
            return user.getId();
        }
        return "anonymous";
    }

    public void evictAll() {
        buckets.clear();
    }
}


