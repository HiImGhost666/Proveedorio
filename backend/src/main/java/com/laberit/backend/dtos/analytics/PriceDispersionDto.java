package com.laberit.backend.dtos.analytics;

import java.math.BigDecimal;

/**
 * Dispersion de precio para SKUs disponibles en 2+ proveedores.
 * dispersionPct = (MAX - MIN) / AVG * 100
 * Alto valor = oportunidad de negociacion.
 */
public record PriceDispersionDto(
    Integer productId,
    String mpn,
    String category,
    int supplierCount,
    BigDecimal minPrice,
    BigDecimal maxPrice,
    BigDecimal avgPrice,
    double dispersionPct
) {}
