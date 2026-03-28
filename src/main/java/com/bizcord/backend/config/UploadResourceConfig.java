package com.bizcord.backend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

@Configuration
@RequiredArgsConstructor
public class UploadResourceConfig implements WebMvcConfigurer {
    private final UploadProperties uploadProperties;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String publicPath = normalizePublicBasePath(uploadProperties.getPublicBasePath());
        String resourcePattern = publicPath + "/**";
        String resourceLocation = Path.of(uploadProperties.getBaseDirectory())
                .toAbsolutePath()
                .normalize()
                .toUri()
                .toString();

        registry.addResourceHandler(resourcePattern)
                .addResourceLocations(resourceLocation.endsWith("/") ? resourceLocation : resourceLocation + "/");
    }

    private String normalizePublicBasePath(String value) {
        if (value == null || value.isBlank()) {
            return "/uploads";
        }

        String normalized = value.startsWith("/") ? value : "/" + value;
        return normalized.endsWith("/") ? normalized.substring(0, normalized.length() - 1) : normalized;
    }
}

