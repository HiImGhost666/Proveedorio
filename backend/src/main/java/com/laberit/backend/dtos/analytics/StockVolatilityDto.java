package com.laberit.backend.dtos.analytics;

/**
 * Volatilidad de stock detectada en ventanas de 24 horas.
 * INCONSISTENCIA_API: stock bajo a 0 y recupero >= 50% del maximo (3+ cambios en 24h)
 * VENTA_PROBABLE: stock bajo significativamente sin recuperacion
 * FLUCTUACION_NORMAL: multiples cambios sin patron claro
 */
public record StockVolatilityDto(
    Integer supplierId,
    String supplierName,
    Integer productId,
    String mpn,
    int changesIn24h,
    int maxStock,
    int minStock,
    String volatilityType
) {}
