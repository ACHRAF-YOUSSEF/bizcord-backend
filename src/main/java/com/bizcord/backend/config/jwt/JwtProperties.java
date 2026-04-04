package com.bizcord.backend.config.jwt;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Getter
@Setter
@Validated
@Component
@ConfigurationProperties(prefix = "app.jwt")
public class JwtProperties {
    @NotBlank(message = "app.jwt.secret-key must be set (set the APP_JWT_SECRET_KEY environment variable)")
    private String secretKey;
    private long accessTokenExpiryMs;
}

