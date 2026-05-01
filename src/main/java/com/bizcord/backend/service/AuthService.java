package com.bizcord.backend.service;

import com.bizcord.backend.config.jwt.JwtProperties;
import com.bizcord.backend.config.jwt.JwtService;
import com.bizcord.backend.dto.AuthRequest;
import com.bizcord.backend.dto.RegisterRequest;
import com.bizcord.backend.dto.TokenPair;
import com.bizcord.backend.entity.RefreshToken;
import com.bizcord.backend.entity.TokenType;
import com.bizcord.backend.entity.User;
import com.bizcord.backend.entity.VerificationToken;
import com.bizcord.backend.repository.RefreshTokenRepository;
import com.bizcord.backend.repository.UserRepository;
import com.bizcord.backend.repository.VerificationTokenRepository;
import com.bizcord.backend.utils.ErrorMessages;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Base64;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {
    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    public static final long REFRESH_TOKEN_EXPIRY_SECONDS = 60L * 60 * 24 * 7;
    private static final long VERIFICATION_TOKEN_EXPIRY_SECONDS = 60L * 15;
    private static final int REFRESH_TOKEN_BYTES = 32;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Value("${app.security.disposable-email-domains:}")
    private String disposableEmailDomainsRaw;

    private Set<String> disposableEmailDomains;

    private final UserRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final AuthenticationManager authenticationManager;
    private final RefreshTokenRepository refreshTokenRepository;
    private final VerificationTokenRepository verificationTokenRepository;
    private final EmailService emailService;

    @PostConstruct
    void init() {
        disposableEmailDomains = Arrays.stream(disposableEmailDomainsRaw.split("[,\\s]+"))
                .map(String::trim)
                .map(String::toLowerCase)
                .filter(s -> !s.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }

    @Transactional
    public void register(RegisterRequest request) {
        String emailDomain = request.email().substring(request.email().indexOf('@') + 1).toLowerCase();
        if (disposableEmailDomains.contains(emailDomain)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Disposable email addresses are not allowed.");
        }

        if (repository.existsByUsername(request.username())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, ErrorMessages.AUTH_USERNAME_ALREADY_USED);
        }

        if (repository.existsByEmail(request.email())) {
            return;
        }

        var user = User.builder()
                .username(request.username())
                .fullName(request.fullName())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .enabled(false)
                .build();
        try {
            repository.save(user);
        } catch (DataIntegrityViolationException _) {
            return;
        }

        String token = issueVerificationToken(user, TokenType.EMAIL_VERIFICATION);
        emailService.sendWelcomeEmail(user);
        emailService.sendVerificationEmail(user, token);
    }

    @Transactional
    public TokenPair authenticate(AuthRequest request) {
        repository.findByEmail(request.email())
                .filter(user -> !user.isEnabled())
                .ifPresent(_ -> {
                    throw new ResponseStatusException(HttpStatus.FORBIDDEN, ErrorMessages.AUTH_ACCOUNT_NOT_ACTIVATED);
                });

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.email(), request.password())
            );
        } catch (DisabledException _) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, ErrorMessages.AUTH_ACCOUNT_NOT_ACTIVATED);
        } catch (BadCredentialsException _) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, ErrorMessages.AUTH_INVALID_CREDENTIALS);
        }
        var user = repository.findByEmail(request.email()).orElseThrow();
        return buildTokenPair(user);
    }

    @Transactional
    public void verifyEmail(String token) {
        VerificationToken vt = verificationTokenRepository.findByToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, ErrorMessages.AUTH_INVALID_VERIFICATION_TOKEN));

        if (vt.getType() != TokenType.EMAIL_VERIFICATION) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ErrorMessages.AUTH_INVALID_VERIFICATION_TOKEN);
        }
        if (vt.isUsed()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ErrorMessages.AUTH_VERIFICATION_TOKEN_USED);
        }
        if (vt.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ErrorMessages.AUTH_VERIFICATION_TOKEN_EXPIRED);
        }

        vt.setUsed(true);
        verificationTokenRepository.save(vt);

        User user = vt.getUser();
        user.setEnabled(true);
        repository.save(user);

        emailService.sendVerificationSuccessEmail(user);
    }

    @Transactional
    public void resendVerification(String email) {
        repository.findByEmail(email)
                .filter(user -> !user.isEnabled())
                .ifPresent(user -> {
                    verificationTokenRepository.deleteAllByUserAndType(user, TokenType.EMAIL_VERIFICATION);
                    String token = issueVerificationToken(user, TokenType.EMAIL_VERIFICATION);
                    emailService.sendVerificationEmail(user, token);
                });
    }

    @Transactional
    public void forgotPassword(String email) {
        repository.findByEmail(email).ifPresent(user -> {
            verificationTokenRepository.deleteAllByUserAndType(user, TokenType.PASSWORD_RESET);
            String token = issueVerificationToken(user, TokenType.PASSWORD_RESET);
            emailService.sendForgotPasswordEmail(user, token);
        });
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        VerificationToken vt = verificationTokenRepository.findByToken(token)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, ErrorMessages.AUTH_INVALID_RESET_TOKEN));

        if (vt.getType() != TokenType.PASSWORD_RESET) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ErrorMessages.AUTH_INVALID_RESET_TOKEN);
        }
        if (vt.isUsed()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ErrorMessages.AUTH_INVALID_RESET_TOKEN);
        }
        if (vt.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ErrorMessages.AUTH_INVALID_RESET_TOKEN);
        }

        vt.setUsed(true);
        verificationTokenRepository.save(vt);

        User user = vt.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        repository.save(user);
    }

    @Transactional
    public TokenPair refresh(String cookieToken) {
        RefreshToken stored = refreshTokenRepository
                .findByToken(cookieToken)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, ErrorMessages.AUTH_INVALID_REFRESH_TOKEN));

        if (stored.isRevoked()) {
            log.warn("Refresh token reuse detected for user {} — revoking all sessions", stored.getUser().getEmail());
            refreshTokenRepository.deleteAllByUser(stored.getUser());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, ErrorMessages.AUTH_REFRESH_TOKEN_REVOKED);
        }
        if (stored.getExpiresAt().isBefore(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, ErrorMessages.AUTH_REFRESH_TOKEN_EXPIRED);
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

    private String issueVerificationToken(User user, TokenType type) {
        VerificationToken entity = VerificationToken.builder()
                .token(UUID.randomUUID().toString())
                .user(user)
                .type(type)
                .expiresAt(LocalDateTime.now().plusSeconds(VERIFICATION_TOKEN_EXPIRY_SECONDS))
                .build();
        verificationTokenRepository.save(entity);
        return entity.getToken();
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
