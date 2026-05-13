package com.laberit.backend.dtos.supplier;

/**
 * DTO de estado de un proveedor con error (para alertas del dashboard).
 */
public record ProviderStatusDto(
    Integer id,
    String name,
    String lastError
) {}
