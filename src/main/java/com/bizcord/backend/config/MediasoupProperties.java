package com.bizcord.backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.mediasoup")
public class MediasoupProperties {
    private String url;
    private String apiSecret;
}
