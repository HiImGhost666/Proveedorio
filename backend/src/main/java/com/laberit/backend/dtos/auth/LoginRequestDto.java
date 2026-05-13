package com.laberit.backend.dtos.auth;

import jakarta.validation.constraints.NotBlank;

/**
 * DTO de entrada para el login. Usado en POST /api/v1/auth/login.
 * Mensajes de validación en español para el frontend.
 */
public record LoginRequestDto(
    @NotBlank(message = "El username no puede estar vacío") String username,
    @NotBlank(message = "La contraseña es obligatoria") String password
) {}
