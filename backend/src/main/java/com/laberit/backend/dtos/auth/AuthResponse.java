package com.laberit.backend.dtos.auth;

import java.util.List;

/**
 * DTO de respuesta de autenticación (login y refresh). Usado en endpoints de auth.
 */
public record AuthResponse(
    String token,
    String tokenType,
    Long expiresIn,
    String username,
    List<String> roles
) {
    public AuthResponse {
        if (tokenType == null) tokenType = "Bearer";
    }
}
