package com.proveedorio.backend.dtos.pricing;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO de respuesta con un registro completo de historial de precios.
 */
public record PriceHistoryResponseDto(
    Integer historyId,
    Integer masterProductId,
    String productName,
    Integer supplierId,
    String supplierName,
    BigDecimal costPrice,
    LocalDateTime registeredAt
) {}
