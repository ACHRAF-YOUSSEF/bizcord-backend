package com.bizcord.backend.controller;

import com.bizcord.backend.config.CookieProperties;
import com.bizcord.backend.config.ratelimit.RateLimit;
import com.bizcord.backend.config.ratelimit.RateLimitKeyType;
import com.bizcord.backend.dto.*;
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
    public ResponseEntity<ApiMessageResponse> register(@Valid @RequestBody RegisterRequest request) {
        service.register(request);
        return ResponseEntity
                .status(CREATED)
                .body(new ApiMessageResponse("Registration successful. Please check your email to verify your account."));
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

    @RateLimit(limit = 5, keyType = RateLimitKeyType.IP)
    @GetMapping("/verify")
    public ResponseEntity<ApiMessageResponse> verifyEmail(@RequestParam String token) {
        service.verifyEmail(token);
        return ResponseEntity.ok(new ApiMessageResponse("Account verified successfully. You can now log in."));
    }

    @RateLimit(limit = 3, keyType = RateLimitKeyType.IP)
    @PostMapping("/resend-verification")
    public ResponseEntity<ApiMessageResponse> resendVerification(@Valid @RequestBody ResendVerificationRequest request) {
        service.resendVerification(request.email());
        return ResponseEntity.ok(new ApiMessageResponse("Verification email sent."));
    }

    @RateLimit(limit = 3, keyType = RateLimitKeyType.IP)
    @PostMapping("/forgot-password")
    public ResponseEntity<ApiMessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        service.forgotPassword(request.email());
        return ResponseEntity.ok(new ApiMessageResponse("If an account with that email exists, a reset link has been sent."));
    }

    @RateLimit(limit = 3, keyType = RateLimitKeyType.IP)
    @PostMapping("/reset-password")
    public ResponseEntity<ApiMessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        service.resetPassword(request.token(), request.newPassword());
        return ResponseEntity.ok(new ApiMessageResponse("Password reset successfully. You can now log in."));
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
        return new AuthResponse(pair.accessToken(), pair.expiresIn());
    }
}
