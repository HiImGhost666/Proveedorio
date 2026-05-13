package com.proveedorio.backend.dtos.auth;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Set;

/**
 * DTO de usuario para listado y actualización. Usado en CRUD de usuarios.
 * El campo password no se serializa en respuestas (solo se acepta en peticiones).
 */
public record UserDto(
    Integer id,
    String username,
    String email,
    String fullName,
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) String password,
    Set<String> roles
) {}
