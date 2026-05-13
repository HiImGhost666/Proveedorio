package com.laberit.backend.dtos.analytics;

import java.math.BigDecimal;

/**
 * Score de estabilidad de precio basado en el coeficiente de variacion (CV = stddev/avg).
 * ESTABLE: CV < 5%
 * MODERADO: CV 5-15%
 * VOLATIL: CV > 15%
 */
public record PriceStabilityDto(
    Integer productId,
    String mpn,
    Integer supplierId,
    String supplierName,
    BigDecimal avgPrice,
    BigDecimal stddevPrice,
    double cvPct,
    String stabilityLabel,
    int pricePoints
) {}
