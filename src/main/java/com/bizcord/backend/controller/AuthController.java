package com.bizcord.backend.controller;

import com.bizcord.backend.config.CookieProperties;
import com.bizcord.backend.config.ratelimit.RateLimit;
import com.bizcord.backend.config.ratelimit.RateLimitKeyType;
import com.bizcord.backend.dto.AuthRequest;
import com.bizcord.backend.dto.AuthResponse;
import com.bizcord.backend.dto.RegisterRequest;
import com.bizcord.backend.dto.TokenPair;
import com.bizcord.backend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static org.springframework.http.HttpStatus.CREATED;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    static final String REFRESH_COOKIE_NAME = "refreshToken";

    private final AuthService service;
    private final CookieProperties cookieProperties;

    @RateLimit(limit = 3, keyType = RateLimitKeyType.IP)
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        TokenPair pair = service.register(request);
        return ResponseEntity
                .status(CREATED)
                .header(HttpHeaders.SET_COOKIE, buildRefreshCookie(pair.refreshToken()).toString())
                .body(toResponse(pair));
    }

    @RateLimit(limit = 5, keyType = RateLimitKeyType.IP)
    @PostMapping("/authenticate")
    public ResponseEntity<AuthResponse> authenticate(@Valid @RequestBody AuthRequest request) {
        TokenPair pair = service.authenticate(request);
        return ResponseEntity
                .ok()
                .header(HttpHeaders.SET_COOKIE, buildRefreshCookie(pair.refreshToken()).toString())
                .body(toResponse(pair));
    }

    @RateLimit(limit = 10, keyType = RateLimitKeyType.IP)
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String cookieToken) {
        if (cookieToken == null || cookieToken.isBlank()) {
            return ResponseEntity.status(401).build();
        }
        TokenPair pair = service.refresh(cookieToken);
        return ResponseEntity
                .ok()
                .header(HttpHeaders.SET_COOKIE, buildRefreshCookie(pair.refreshToken()).toString())
                .body(toResponse(pair));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String cookieToken) {
        service.logout(cookieToken);
        return ResponseEntity
                .noContent()
                .header(HttpHeaders.SET_COOKIE, clearRefreshCookie().toString())
                .build();
    }

    private ResponseCookie buildRefreshCookie(String value) {
        return ResponseCookie.from(REFRESH_COOKIE_NAME, value)
                .httpOnly(true)
                .secure(cookieProperties.isSecure())
                .sameSite(cookieProperties.getSameSite())
                .path(cookieProperties.getPath())
                .maxAge(cookieProperties.getMaxAge())
                .build();
    }

    private ResponseCookie clearRefreshCookie() {
        return ResponseCookie.from(REFRESH_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(cookieProperties.isSecure())
                .sameSite(cookieProperties.getSameSite())
                .path(cookieProperties.getPath())
                .maxAge(0)
                .build();
    }

    private static AuthResponse toResponse(TokenPair pair) {
        return AuthResponse.builder()
                .token(pair.accessToken())
                .expiresIn(pair.expiresIn())
                .build();
    }
}
