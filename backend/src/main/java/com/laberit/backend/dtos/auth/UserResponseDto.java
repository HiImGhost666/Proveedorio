package com.laberit.backend.dtos.auth;

/**
 * DTO de respuesta tras registro o consulta de usuario (sin password ni roles sensibles).
 */
public record UserResponseDto(
    Integer userId,
    String username,
    String email,
    String fullName
) {}
