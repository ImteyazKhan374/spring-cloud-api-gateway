package com.abkatk.apigateway.security;

import com.abkatk.apigateway.util.JwtUtil; // Import your JwtUtil to use its validation and extraction methods
import org.springframework.security.authentication.ReactiveAuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class JwtAuthenticationManager implements ReactiveAuthenticationManager {

    private final JwtUtil jwtUtil; // Inject your JwtUtil for JWT operations

    public JwtAuthenticationManager(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    /**
     * Authenticates the provided Authentication object (which contains the JWT string).
     * This method is part of Spring Security's reactive authentication flow.
     *
     * @param authentication An unauthenticated Authentication object (from BearerTokenServerAuthenticationConverter)
     * where the principal is the JWT string.
     * @return A Mono emitting an authenticated Authentication object if the JWT is valid,
     * or a Mono emitting an AuthenticationException if validation fails.
     */
    @Override
    public Mono<Authentication> authenticate(Authentication authentication) {
        // Get the JWT token string from the principal, as it's set there by BearerTokenServerAuthenticationConverter
        String authToken = authentication.getPrincipal().toString();

        // Use Mono.fromCallable to wrap synchronous JWT validation into a reactive stream.
        // This ensures that any exceptions thrown by jwtUtil.extractUsername are caught
        // and propagated as an error in the reactive chain.
        return Mono.fromCallable(() -> {
            // Attempt to extract the username. If the token is invalid (expired, bad signature, etc.),
            // jwtUtil.extractUsername (via extractAllClaims) will throw a RuntimeException.
            String username = jwtUtil.extractUsername(authToken);

            // --- Authorization Logic (Extracting Roles/Authorities) ---
            // This part assumes your JWT includes a "roles" claim as a List of Strings.
            // Adjust the claim name ("roles") and type (List.class) based on your JWT's structure.
            List<String> roles = jwtUtil.extractClaim(authToken, claims -> claims.get("roles", List.class));
            if (roles == null) {
                roles = List.of(); // Default to no roles if the claim is missing
            }

            // Convert role strings into Spring Security's GrantedAuthority objects.
            // It's a common practice to prefix roles with "ROLE_" (e.g., "ADMIN" becomes "ROLE_ADMIN").
            List<SimpleGrantedAuthority> authorities = roles.stream()
                    .map(role -> new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()))
                    .collect(Collectors.toList());

            // --- Return Authenticated Token ---
            // Create and return an authenticated UsernamePasswordAuthenticationToken.
            // Explicitly cast to Authentication to resolve the type mismatch.
            return (Authentication) new UsernamePasswordAuthenticationToken(username, null, authorities);
        })
        // --- Error Handling for Authentication Failures ---
        // If any RuntimeException occurs during token validation or claim extraction (e.g., ExpiredJwtException),
        // it's caught here and converted into a Spring Security AuthenticationException.
        // This ensures Spring Security's error handling (e.g., GlobalErrorWebExceptionHandler) can process it.
        .onErrorResume(e -> Mono.error(new org.springframework.security.core.AuthenticationException("Authentication failed: " + e.getMessage(), e) {}));
    }
}
