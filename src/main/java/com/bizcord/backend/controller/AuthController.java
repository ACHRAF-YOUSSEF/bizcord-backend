package com.bizcord.backend.controller;

import com.bizcord.backend.config.ratelimit.RateLimit;
import com.bizcord.backend.config.ratelimit.RateLimitKeyType;
import com.bizcord.backend.dto.AuthRequest;
import com.bizcord.backend.dto.AuthResponse;
import com.bizcord.backend.dto.RegisterRequest;
import com.bizcord.backend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.http.HttpStatus.CREATED;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService service;

    @RateLimit(limit = 3, keyType = RateLimitKeyType.IP)
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity
                .status(CREATED)
                .body(service.register(request));
    }

    @RateLimit(limit = 5, keyType = RateLimitKeyType.IP)
    @PostMapping("/authenticate")
    public ResponseEntity<AuthResponse> authenticate(@Valid @RequestBody AuthRequest request) {
        return ResponseEntity.ok(service.authenticate(request));
    }
}
