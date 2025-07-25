package com.abkatk.apigateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary; // Import for @Primary
import reactor.core.publisher.Mono;

@Configuration
public class RateLimiterConfig {

	/**
	 * Defines a KeyResolver based on the authenticated user's principal name. This
	 * means rate limits will be applied per authenticated user. Requires Spring
	 * Security to be enabled and authentication to be successful. Marked
	 * as @Primary to resolve ambiguity when multiple KeyResolver beans are present.
	 */
//	@Bean
//	@Primary // Mark this bean as the primary choice when a single KeyResolver is required
//	KeyResolver userKeyResolver() {
//		// Uses the authenticated user's name as the key for rate limiting.
//		// If no user is authenticated, it might return an empty Mono,
//		// which means the request won't be rate-limited by this resolver.
//		// You might want a fallback to IP in such cases.
//		return exchange -> exchange.getPrincipal().map(principal -> principal.getName())
//				.switchIfEmpty(Mono.error(new IllegalStateException("User not authenticated for rate limiting")));
//		// Or switchIfEmpty(Mono.just("anonymous")); for anonymous users
//	}

	/**
	 * Defines a KeyResolver based on the client's remote IP address. This means
	 * rate limits will be applied per IP address.
	 */
	@Primary
	@Bean
	KeyResolver ipKeyResolver() {
		// Uses the client's IP address as the key for rate limiting.
		return exchange -> Mono.just(exchange.getRequest().getRemoteAddress().getAddress().getHostAddress());
	}

	// You can define other KeyResolvers based on custom headers, request
	// parameters, etc.
	// Example: KeyResolver based on a custom header
	/*
	 * @Bean public KeyResolver headerKeyResolver() { return exchange ->
	 * Mono.just(exchange.getRequest().getHeaders().getFirst("X-Custom-Client-Id"));
	 * }
	 */
}
