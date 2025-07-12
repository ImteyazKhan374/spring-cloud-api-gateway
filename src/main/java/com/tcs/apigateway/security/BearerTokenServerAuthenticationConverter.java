package com.tcs.apigateway.security;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.server.authentication.ServerAuthenticationConverter;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class BearerTokenServerAuthenticationConverter implements ServerAuthenticationConverter {

    private static final String BEARER_PREFIX = "Bearer ";

    /**
     * Converts the ServerWebExchange into an Authentication object by extracting the JWT from the Authorization header.
     * This method is part of Spring Security's reactive authentication flow.
     *
     * @param exchange The current server web exchange.
     * @return A Mono emitting an unauthenticated UsernamePasswordAuthenticationToken containing the JWT string as principal,
     * or an empty Mono if the Authorization header is missing or not in Bearer format.
     */
    @Override
    public Mono<Authentication> convert(ServerWebExchange exchange) {
        // Retrieve the Authorization header from the request
        return Mono.justOrEmpty(exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION))
                // Filter to ensure the header starts with "Bearer "
                .filter(header -> header.startsWith(BEARER_PREFIX))
                // Extract the actual JWT token by removing the "Bearer " prefix
                .map(header -> header.substring(BEARER_PREFIX.length()))
                // Create an unauthenticated UsernamePasswordAuthenticationToken.
                // The JWT token string is set as the principal (credentials are null for now).
                // This token will then be passed to the ReactiveAuthenticationManager for actual authentication.
                .map(token -> new UsernamePasswordAuthenticationToken(token, null));
    }
}