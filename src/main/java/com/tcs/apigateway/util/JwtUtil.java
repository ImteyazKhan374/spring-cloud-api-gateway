package com.tcs.apigateway.util;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

import javax.crypto.SecretKey;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

// Assuming this DTO exists for generating tokens
import com.tcs.apigateway.dto.AuthRequest;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm; // Explicit import for SignatureAlgorithm
import io.jsonwebtoken.UnsupportedJwtException;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;

@Component
public class JwtUtil {

	@Value("${jwt.secret}")
	private String secretString;

	private SecretKey signingKey;

	private SecretKey getSigningKey() {
		if (this.signingKey == null) {
			this.signingKey = Keys.hmacShaKeyFor(secretString.getBytes(StandardCharsets.UTF_8));
		}
		return this.signingKey;
	}

	/**
	 * Generates a JWT token for the given authentication request.
	 *
	 * @param authRequest The authentication request containing username and
	 *                    tenantId.
	 * @return The generated JWT token.
	 */
	public String generateToken(AuthRequest authRequest) {
		Map<String, Object> claims = new HashMap<>();
		claims.put("tenantId", authRequest.getTenantId());
		claims.put("username", authRequest.getUsername());

		return Jwts.builder().setClaims(claims).setSubject(authRequest.getUsername())
				.setIssuedAt(new Date(System.currentTimeMillis()))
				.setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 8)) // 8 hours expiration
				.signWith(getSigningKey(), SignatureAlgorithm.HS256) // Line 59: SignatureAlgorithm should now be found
				.compact();
	}

	/**
	 * Extracts all claims from a JWT token.
	 *
	 * @param token The JWT token string.
	 * @return Claims object containing all token claims.
	 * @throws RuntimeException if the token is invalid (e.g., expired, malformed,
	 *                          bad signature).
	 */
	private Claims extractAllClaims(String token) {
		try {
			return Jwts.parser().setSigningKey(getSigningKey()).build().parseClaimsJws(token).getBody();
		} catch (SignatureException | MalformedJwtException | ExpiredJwtException | UnsupportedJwtException
				| IllegalArgumentException e) {
			System.err.println("JWT Validation Error: " + e.getMessage());
			throw new RuntimeException("Invalid or expired JWT token", e);
		}
	}

	/**
	 * Extracts a specific claim from the token using a claims resolver function.
	 *
	 * @param token          The JWT token string.
	 * @param claimsResolver A function to resolve the desired claim from the Claims
	 *                       object.
	 * @param <T>            The type of the claim.
	 * @return The extracted claim.
	 */
	public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
		final Claims claims = extractAllClaims(token);
		return claimsResolver.apply(claims);
	}

	/**
	 * Extracts the username (subject) from the JWT token.
	 *
	 * @param token The JWT token string.
	 * @return The username.
	 */
	public String extractUsername(String token) {
		return extractClaim(token, Claims::getSubject);
	}

	/**
	 * Extracts the tenantId custom claim from the JWT token.
	 *
	 * @param token The JWT token string.
	 * @return The tenantId.
	 */
	public String extractTenantId(String token) {
		return extractClaim(token, claims -> (String) claims.get("tenantId"));
	}

	/**
	 * Checks if the JWT token is expired.
	 *
	 * @param token The JWT token string.
	 * @return True if the token is expired, false otherwise.
	 */
	private Boolean isTokenExpired(String token) {
		try {
			final Date expiration = extractClaim(token, Claims::getExpiration);
			return expiration.before(new Date());
		} catch (ExpiredJwtException e) {
			return true;
		} catch (RuntimeException e) {
			return true;
		}
	}

	/**
	 * Validates the JWT token against user details. This method checks: 1. If the
	 * token is valid (signature, format). 2. If the username extracted from the
	 * token matches the UserDetails username. 3. If the token has not expired.
	 *
	 * @param token       The JWT token string.
	 * @param userDetails The UserDetails object to compare against.
	 * @return True if the token is valid for the given user, false otherwise.
	 */
	public boolean validateToken(String token, UserDetails userDetails) {
		try {
			final String username = extractUsername(token);
			return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
		} catch (RuntimeException e) {
			return false;
		}
	}
}
