package com.bizcord.backend.config.ratelimit;

import com.bizcord.backend.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public boolean preHandle(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler
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
        String endpoint = request.getMethod() + ":" + request.getRequestURI();

        return switch (rateLimit.keyType()) {
            case IP -> endpoint + ":ip:" + resolveIp(request);
            case UID -> endpoint + ":uid:" + resolveUserId();
            case IP_AND_UID -> endpoint + ":ip:" + resolveIp(request) + ":uid:" + resolveUserId();
        };
    }

    private String resolveIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isBlank()) {
            return xForwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
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

    public int getBucketCount() {
        return buckets.size();
    }
}


