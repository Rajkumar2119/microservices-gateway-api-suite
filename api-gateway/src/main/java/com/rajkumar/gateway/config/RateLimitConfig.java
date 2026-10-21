package com.rajkumar.gateway.config;

import java.net.InetSocketAddress;
import java.security.Principal;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

@Configuration
public class RateLimitConfig {

    /**
     * Rate-limit bucket per signed-in user, or per client IP for anonymous calls
     * (login, register, catalogue). Buckets live in Redis so every gateway instance shares them.
     */
    @Bean
    @Primary
    KeyResolver userOrIpKeyResolver() {
        return exchange -> exchange.getPrincipal()
                .map(Principal::getName)
                .map(name -> "user:" + name)
                .switchIfEmpty(Mono.fromSupplier(() -> {
                    InetSocketAddress address = exchange.getRequest().getRemoteAddress();
                    return "ip:" + (address == null ? "unknown" : address.getHostString());
                }));
    }
}
