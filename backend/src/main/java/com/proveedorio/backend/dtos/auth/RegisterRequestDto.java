package com.proveedorio.backend.dtos.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO de entrada para registro de nuevo usuario. Usado en POST /api/v1/auth/register.
 * No incluye roles para evitar autoasignación.
 */
public record RegisterRequestDto(
    @NotNull @Size(min = 5) String username,
    @NotBlank(message = "La contraseña es obligatoria") String password,
    @NotBlank(message = "El nombre completo es obligatorio") String fullName,
    @Email(message = "El email no es válido") String email
) {}
