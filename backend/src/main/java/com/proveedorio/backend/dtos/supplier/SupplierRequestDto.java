package com.proveedorio.backend.dtos.supplier;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * DTO de entrada para crear o actualizar un proveedor.
 * Usado en POST/PUT de proveedores. Mensajes de validación en español para el frontend.
 */
public record SupplierRequestDto(
    @NotBlank(message = "El nombre no puede estar vacío") String name,
    @NotBlank String baseUrlApi,
    String contact,
    String email,
    String schedule,
    String phone,
    String website,
    String country,
    String defaultCurrency,
    String apiKey,
    @NotNull Boolean active,
    @NotNull Boolean supportsBulkSync,
    String catalogEndpoint,
    String detailEndpoint,
    String searchEndpoint
) {}
