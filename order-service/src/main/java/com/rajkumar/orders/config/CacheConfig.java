package com.rajkumar.orders.config;

import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.cache.RedisCacheManagerBuilderCustomizer;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;

/** The product catalogue is read far more often than it changes, so it is cached in Redis. */
@Configuration
@EnableCaching
public class CacheConfig {

    public static final String PRODUCTS = "products";

    @Bean
    RedisCacheManagerBuilderCustomizer productCacheCustomizer(
            @Value("${orders.cache.products-ttl:PT10M}") Duration ttl) {
        return builder -> builder.withCacheConfiguration(PRODUCTS,
                RedisCacheConfiguration.defaultCacheConfig().entryTtl(ttl).disableCachingNullValues());
    }
}
