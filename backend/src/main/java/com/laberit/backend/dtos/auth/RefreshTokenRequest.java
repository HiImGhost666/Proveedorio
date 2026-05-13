package com.laberit.backend.dtos.auth;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO para la solicitud de refresco de token. Usado en POST /api/v1/auth/refresh.
 */
public record RefreshTokenRequest(
    @NotBlank(message = "El token de refresco es obligatorio") String refreshToken
) {}
