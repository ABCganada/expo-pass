package com.coderhan.lastmission.config;

import java.util.List;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "lastmission.cors")
public record CorsProperties(List<String> allowedOrigins) {
    public String[] allowedOriginsArray() {
        return allowedOrigins.toArray(String[]::new);
    }
}
