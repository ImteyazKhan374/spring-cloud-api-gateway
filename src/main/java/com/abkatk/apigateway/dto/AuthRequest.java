package com.abkatk.apigateway.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

// This is an example DTO. Adjust fields as per your actual authentication request.
@Data // Lombok annotation for getters, setters, toString, equals, hashCode
@AllArgsConstructor // Lombok annotation for an all-args constructor
@NoArgsConstructor // Lombok annotation for a no-args constructor
public class AuthRequest {
    private String username;
    private String password; // Not used in JWT validation, but for initial authentication
    private String tenantId; // Example custom field
}
