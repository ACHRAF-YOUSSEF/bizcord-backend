package com.bizcord.backend.service;

import com.bizcord.backend.config.jwt.JwtService;
import com.bizcord.backend.dto.AuthRequest;
import com.bizcord.backend.dto.AuthResponse;
import com.bizcord.backend.dto.RefreshTokenRequest;
import com.bizcord.backend.dto.RegisterRequest;
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
    private static final long REFRESH_TOKEN_EXPIRY_SECONDS = 60L * 60 * 24 * 7;
    private static final int REFRESH_TOKEN_BYTES = 32;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository repository;
    public final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenRepository refreshTokenRepository;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        var user = User
                .builder()
                .username(request.getUsername())
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .build();

        repository.save(user);

        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse authenticate(AuthRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        var user = repository
                .findByEmail(request.getEmail())
                .orElseThrow();

        return buildAuthResponse(user);
    }

    @Transactional
    public AuthResponse refresh(RefreshTokenRequest request) {
        RefreshToken stored = refreshTokenRepository
                .findByToken(request.getRefreshToken())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));

        if (stored.isRevoked()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token has been revoked");
        }

        if (stored.getExpiresAt().isBefore(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token has expired");
        }

        stored.setRevoked(true);
        refreshTokenRepository.save(stored);

        return buildAuthResponse(stored.getUser());
    }

    private AuthResponse buildAuthResponse(User user) {
        String accessToken = jwtService.generateToken(user);
        String rawRefreshToken = issueRefreshToken(user);

        return AuthResponse
                .builder()
                .token(accessToken)
                .refreshToken(rawRefreshToken)
                .expiresIn(JwtService.ACCESS_TOKEN_EXPIRY_MS / 1000)
                .build();
    }

    private String issueRefreshToken(User user) {
        for (int attempt = 0; attempt < 3; attempt++) {
            byte[] bytes = new byte[REFRESH_TOKEN_BYTES];
            SECURE_RANDOM.nextBytes(bytes);
            String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);

            RefreshToken entity = RefreshToken
                    .builder()
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
