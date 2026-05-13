package com.proveedorio.backend.dtos.pricing;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO de un registro de historial de precio (precio, fecha, tendencia).
 * Usado en GET /api/v1/productos/{id}/historial-precios.
 */
public record PriceHistoryItemDto(
    BigDecimal price,
    LocalDateTime registeredAt,
    String trend
) {}
