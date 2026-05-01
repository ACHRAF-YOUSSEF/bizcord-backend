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
    @NotBlank(message = "app.jwt.private-key-path must be set")
    private String privateKeyPath;

    @NotBlank(message = "app.jwt.public-key-path must be set")
    private String publicKeyPath;

    private long accessTokenExpiryMs;
}
