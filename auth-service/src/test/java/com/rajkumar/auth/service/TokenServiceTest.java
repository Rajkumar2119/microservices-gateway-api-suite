package com.rajkumar.auth.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.rajkumar.auth.config.JwtProperties;
import com.rajkumar.auth.config.SecurityConfig;
import com.rajkumar.auth.domain.AppUser;
import com.rajkumar.auth.domain.Role;
import java.time.Duration;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

class TokenServiceTest {

    private static final String SECRET = "unit-test-secret-0123456789-abcdefghij";

    private final SecurityConfig config = new SecurityConfig();
    private final JwtProperties properties = new JwtProperties(SECRET, "http://auth-service", Duration.ofMinutes(15));
    private final SecretKey key = config.jwtSigningKey(properties);
    private final TokenService tokenService = new TokenService(config.jwtEncoder(key), properties);
    private final JwtDecoder decoder = config.jwtDecoder(key, properties);

    @Test
    void issuedTokenCarriesSubjectRolesAndExpiry() {
        AppUser user = new AppUser("raj", "raj@example.com", "hash", Role.ADMIN);

        TokenService.IssuedToken token = tokenService.issue(user);
        Jwt jwt = decoder.decode(token.value());

        assertThat(token.expiresInSeconds()).isEqualTo(900);
        assertThat(jwt.getSubject()).isEqualTo("raj");
        assertThat(jwt.getClaimAsStringList("roles")).containsExactly("ADMIN");
        assertThat(jwt.getIssuer()).hasToString("http://auth-service");
        assertThat(jwt.getExpiresAt()).isAfter(jwt.getIssuedAt());
    }

    @Test
    void tokenSignedWithAnotherKeyIsRejected() {
        JwtProperties other = new JwtProperties("a-completely-different-secret-0123456789", "http://auth-service",
                Duration.ofMinutes(15));
        SecretKey otherKey = config.jwtSigningKey(other);
        String forged = new TokenService(config.jwtEncoder(otherKey), other)
                .issue(new AppUser("mallory", "m@example.com", "hash", Role.ADMIN)).value();

        assertThatThrownBy(() -> decoder.decode(forged)).isInstanceOf(JwtException.class);
    }

    @Test
    void tokenFromAnotherIssuerIsRejected() {
        JwtProperties otherIssuer = new JwtProperties(SECRET, "http://someone-else", Duration.ofMinutes(15));
        String token = new TokenService(config.jwtEncoder(key), otherIssuer)
                .issue(new AppUser("raj", "raj@example.com", "hash", Role.USER)).value();

        assertThatThrownBy(() -> decoder.decode(token)).isInstanceOf(JwtException.class);
    }

    @Test
    void shortSecretIsRefused() {
        assertThatThrownBy(() -> new JwtProperties("too-short", "iss", Duration.ofMinutes(1)))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
