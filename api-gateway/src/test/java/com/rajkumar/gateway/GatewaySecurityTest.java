package com.rajkumar.gateway;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.rajkumar.gateway.config.CorrelationIdFilter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.reactive.server.WebTestClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
@ActiveProfiles("test")
class GatewaySecurityTest {

    private static final String SECRET = "test-secret-for-unit-tests-only-0123456789";

    @Autowired
    private WebTestClient client;

    @Test
    void protectedRouteWithoutTokenIsRejectedAtTheEdge() {
        client.get().uri("/api/orders")
                .exchange()
                .expectStatus().isUnauthorized()
                .expectHeader().exists(CorrelationIdFilter.HEADER);
    }

    @Test
    void tokenSignedWithWrongKeyIsRejected() {
        client.get().uri("/api/orders")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token("some-other-secret-0123456789-abcdefgh",
                        "http://auth-service", Instant.now().plusSeconds(300)))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void expiredTokenIsRejected() {
        client.get().uri("/api/orders")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(SECRET, "http://auth-service",
                        Instant.now().minusSeconds(600)))
                .exchange()
                .expectStatus().isUnauthorized();
    }

    @Test
    void validTokenPassesSecurityAndIsRouted() {
        // Nothing listens on the test upstream, so getting past security shows up as a 5xx, not a 401.
        client.get().uri("/api/orders")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token(SECRET, "http://auth-service",
                        Instant.now().plusSeconds(300)))
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    void loginAndCatalogueArePublic() {
        client.post().uri("/api/auth/login")
                .exchange()
                .expectStatus().is5xxServerError();
        client.get().uri("/api/products")
                .exchange()
                .expectStatus().is5xxServerError();
    }

    @Test
    void healthIsPublicAndCallerCorrelationIdIsKept() {
        client.get().uri("/actuator/health")
                .header(CorrelationIdFilter.HEADER, "test-correlation-1234")
                .exchange()
                .expectStatus().isOk()
                .expectHeader().valueEquals(CorrelationIdFilter.HEADER, "test-correlation-1234");
    }

    private static String token(String secret, String issuer, Instant expiresAt) {
        NimbusJwtEncoder encoder = new NimbusJwtEncoder(
                new ImmutableSecret<>(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256")));
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(issuer)
                .subject("raj")
                .issuedAt(expiresAt.minusSeconds(900))
                .expiresAt(expiresAt)
                .claim("roles", List.of("USER"))
                .build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();
    }
}
