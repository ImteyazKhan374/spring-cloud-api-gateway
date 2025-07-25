package com.abkatk.apigateway.util; // Ensure this package matches your project structure

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class JwtGenerator {

    // This secret MUST match the 'jwt.secret' in your API Gateway's application.yml
    private static final String SECRET = "thisisaverylongandsecuresecretkeyforjwtauthenticationtesting"; // Must match

	private SecretKey signingKey;

    
    private SecretKey getSigningKey() {
		if (this.signingKey == null) {
			this.signingKey = Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8));
		}
		return this.signingKey;
	}

    @SuppressWarnings("deprecation")
	public  String generateToken(String username, List<String> roles) {
    	Map<String, Object> claims = new HashMap<>();
		claims.put("tenantId", "sincdev"); // ✅ custom claim
		claims.put("username", "admin"); // optional, subject will also contain this

		return Jwts.builder().setClaims(claims).setSubject(username)
				.setIssuedAt(new Date(System.currentTimeMillis()))
				.setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 2400)) // 8 hrs
				.signWith(getSigningKey(), SignatureAlgorithm.HS256).compact();
    }

    public static void main(String[] args) {
    	JwtGenerator jwtGenerator = new JwtGenerator();
        // Generate a token for a user with "USER" role
        String userToken = jwtGenerator.generateToken("admin", List.of("USER"));
        System.out.println("Generated User JWT: " + userToken);

        // Generate a token for an admin user with "USER" and "ADMIN" roles
        String adminToken = jwtGenerator.generateToken("admin", List.of("USER", "ADMIN"));
        System.out.println("Generated Admin JWT: " + adminToken);

        // You can paste these tokens into jwt.io to inspect their claims and verify expiration.
    }
}
