package com.rajkumar.auth.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * @param secret   shared HMAC secret (at least 32 bytes) used to sign tokens; supply via JWT_SECRET
 * @param issuer   value of the {@code iss} claim, checked by every service
 * @param tokenTtl how long an access token is valid
 */
@ConfigurationProperties(prefix = "security.jwt")
public record JwtProperties(String secret, String issuer, Duration tokenTtl) {

    public JwtProperties {
        if (secret == null || secret.getBytes(java.nio.charset.StandardCharsets.UTF_8).length < 32) {
            throw new IllegalArgumentException("security.jwt.secret must be at least 32 bytes for HS256");
        }
        if (issuer == null || issuer.isBlank()) {
            issuer = "auth-service";
        }
        if (tokenTtl == null) {
            tokenTtl = Duration.ofMinutes(30);
        }
    }
}
