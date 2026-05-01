package com.bizcord.backend.config.jwt;

import com.bizcord.backend.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;

@Service
public class JwtService {
    private static final String ISSUER = "bizcord";
    private static final String AUDIENCE = "bizcord-api";
    private static final String TOKEN_TYPE_CLAIM = "token_type";
    private static final String TOKEN_TYPE_ACCESS = "ACCESS";

    private final JwtProperties jwtProperties;
    private final PrivateKey privateKey;
    private final PublicKey publicKey;

    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        try {
            this.privateKey = KeyUtils.loadPrivateKey(Path.of(jwtProperties.getPrivateKeyPath()));
            this.publicKey = KeyUtils.loadPublicKey(Path.of(jwtProperties.getPublicKeyPath()));
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load JWT RSA keys: " + e.getMessage(), e);
        }
    }

    @PostConstruct
    void validateConfig() {
        if (jwtProperties.getAccessTokenExpiryMs() <= 0) {
            throw new IllegalStateException("app.jwt.access-token-expiry-ms must be > 0");
        }
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        return claimsResolver.apply(extractAllClaims(token));
    }

    public String generateToken(User user) {
        return generateToken(new HashMap<>(), user);
    }

    public String generateToken(Map<String, Object> extraClaims, User user) {
        Map<String, Object> claims = new HashMap<>(extraClaims);
        claims.put(TOKEN_TYPE_CLAIM, TOKEN_TYPE_ACCESS);

        return Jwts.builder()
                .claims(claims)
                .issuer(ISSUER)
                .audience()
                .add(AUDIENCE)
                .and()
                .subject(user.getEmail())
                .id(UUID.randomUUID().toString())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + jwtProperties.getAccessTokenExpiryMs()))
                .signWith(privateKey)
                .compact();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        Claims claims = extractAllClaims(token);
        String username = claims.getSubject();
        boolean notExpired = !claims.getExpiration().before(new Date());
        boolean isAccessToken = TOKEN_TYPE_ACCESS.equals(claims.get(TOKEN_TYPE_CLAIM));
        return username.equals(userDetails.getUsername()) && notExpired && isAccessToken;
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(publicKey)
                .requireIssuer(ISSUER)
                .requireAudience(AUDIENCE)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
