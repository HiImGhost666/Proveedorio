package com.laberit.backend.dtos.dashboard;

import java.math.BigDecimal;

/**
 * DTO de movimiento de precio (subida/bajada) para alertas del dashboard.
 * Incluye el identificador del producto maestro para poder navegar al detalle.
 */
public record TopMoverDto(
    Integer productId,
    String productName,
    String supplierName,
    BigDecimal previousPrice,
    BigDecimal currentPrice,
    double percentChange
) {}
