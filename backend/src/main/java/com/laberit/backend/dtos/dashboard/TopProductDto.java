package com.laberit.backend.dtos.dashboard;

import java.math.BigDecimal;

/**
 * DTO de producto en ranking de precios.
 * Incluye el identificador del producto maestro para poder navegar al detalle en el frontend.
 */
public record TopProductDto(
    Integer productId,
    String name,
    String supplierName,
    BigDecimal price
) {}
