// src/main/java/com/tcs/apigateway/config/CacheConfig.java
package com.tcs.apigateway.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching // Enables Spring's caching abstraction
public class CacheConfig {

    /**
     * Configures a CaffeineCacheManager for the API Gateway.
     * This cache manager will manage caches with a maximum size and a time-to-live.
     *
     * @return Configured CacheManager instance.
     */
    @Bean
    public CacheManager cacheManager() {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager("apiResponses"); // Define a cache name
        cacheManager.setCaffeine(caffeineCacheBuilder());
        return cacheManager;
    }

    /**
     * Builds the Caffeine cache configuration.
     * Sets a maximum size for the cache and an expiration time for entries.
     *
     * @return Caffeine builder with desired settings.
     */
    Caffeine<Object, Object> caffeineCacheBuilder() {
        return Caffeine.newBuilder()
                .maximumSize(1000) // Maximum number of entries in the cache
                .expireAfterAccess(5, TimeUnit.MINUTES) // Entries expire 5 minutes after last access
                .recordStats(); // Record cache statistics for monitoring (optional)
    }
}
