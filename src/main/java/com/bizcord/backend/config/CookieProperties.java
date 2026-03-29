package com.bizcord.backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.cookie")
public class CookieProperties {
    private boolean secure = true;
    private String sameSite = "Strict";
    private String path = "/api/auth";
    private long maxAge = 60L * 60 * 24 * 7;
}

