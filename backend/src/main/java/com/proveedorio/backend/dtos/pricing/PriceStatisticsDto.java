package com.proveedorio.backend.dtos.pricing;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO para análisis y estadísticas de precios (BI). Métricas agregadas por producto/proveedor.
 */
public record PriceStatisticsDto(
    BigDecimal averagePrice,
    BigDecimal minPrice,
    BigDecimal maxPrice,
    BigDecimal percentVariation,
    BigDecimal currentPrice,
    BigDecimal initialPrice,
    LocalDateTime mostRecentDate,
    LocalDateTime oldestDate,
    String productName,
    String supplierName,
    Integer totalRecords
) {
    /** Crea una instancia vacía cuando no hay datos. */
    public static PriceStatisticsDto empty() {
        return new PriceStatisticsDto(
            BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
            BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
            null, null, "", "", 0
        );
    }
}
