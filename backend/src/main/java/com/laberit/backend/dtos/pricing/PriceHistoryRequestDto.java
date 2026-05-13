package com.laberit.backend.dtos.pricing;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO de entrada para registrar un cambio de precio en el historial.
 * Validado en el controlador para garantizar integridad de datos y mensajes en español.
 */
public record PriceHistoryRequestDto(
    @NotNull(message = "El identificador de producto es obligatorio") Integer masterProductId,
    @NotNull(message = "El identificador de proveedor es obligatorio") Integer supplierId,
    @NotNull(message = "El precio de coste es obligatorio") @Min(value = 0, message = "El precio de coste no puede ser negativo") BigDecimal costPrice,
    LocalDateTime registeredAt
) {}
