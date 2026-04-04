package com.bizcord.backend.service;

import com.bizcord.backend.config.jwt.JwtProperties;
import com.bizcord.backend.config.jwt.JwtService;
import com.bizcord.backend.dto.AuthRequest;
import com.bizcord.backend.dto.RegisterRequest;
import com.bizcord.backend.dto.TokenPair;
import com.bizcord.backend.entity.RefreshToken;
import com.bizcord.backend.entity.User;
import com.bizcord.backend.repository.RefreshTokenRepository;
import com.bizcord.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class AuthService {
    public static final long REFRESH_TOKEN_EXPIRY_SECONDS = 60L * 60 * 24 * 7;
    private static final int REFRESH_TOKEN_BYTES = 32;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository repository;
    public final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public TokenPair register(RegisterRequest request) {
        var user = User.builder()
                .username(request.getUsername())
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .build();
        repository.save(user);
        return buildTokenPair(user);
    }

    @Transactional
    public TokenPair authenticate(AuthRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );
        var user = repository.findByEmail(request.getEmail()).orElseThrow();
        return buildTokenPair(user);
    }

    @Transactional
    public TokenPair refresh(String cookieToken) {
        RefreshToken stored = refreshTokenRepository
                .findByToken(cookieToken)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));

        if (stored.isRevoked()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token has been revoked");
        }
        if (stored.getExpiresAt().isBefore(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token has expired");
        }

        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        return buildTokenPair(stored.getUser());
    }

    @Transactional
    public void logout(String cookieToken) {
        if (cookieToken == null || cookieToken.isBlank()) return;
        refreshTokenRepository.findByToken(cookieToken).ifPresent(rt -> {
            rt.setRevoked(true);
            refreshTokenRepository.save(rt);
        });
    }

    private TokenPair buildTokenPair(User user) {
        String accessToken  = jwtService.generateToken(user);
        String refreshToken = issueRefreshToken(user);
        return new TokenPair(accessToken, refreshToken, jwtProperties.getAccessTokenExpiryMs() / 1000);
    }

    private String issueRefreshToken(User user) {
        for (int attempt = 0; attempt < 3; attempt++) {
            byte[] bytes = new byte[REFRESH_TOKEN_BYTES];
            SECURE_RANDOM.nextBytes(bytes);
            String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

            RefreshToken entity = RefreshToken.builder()
                    .token(raw)
                    .user(user)
                    .expiresAt(Instant.now().plusSeconds(REFRESH_TOKEN_EXPIRY_SECONDS))
                    .build();

            try {
                refreshTokenRepository.save(entity);
                return raw;
            } catch (DataIntegrityViolationException _) {
                // token collision – generate a new one
            }
        }
        throw new IllegalStateException("Failed to generate a unique refresh token after 3 attempts");
    }
}
