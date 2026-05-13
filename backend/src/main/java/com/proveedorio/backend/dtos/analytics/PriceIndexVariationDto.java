package com.proveedorio.backend.dtos.analytics;

import java.math.BigDecimal;

/**
 * DTO de variacion del indice de precios por proveedor y mes.
 *
 * Generado mediante window function LAG() sobre historial_precios.
 * Permite detectar que proveedores estan subiendo costes mes a mes
 * y filtrar outliers de mapeo de API (variaciones absurdas > umbral).
 *
 * @param supplierId    ID del proveedor
 * @param supplierName  Nombre del proveedor
 * @param productId     ID del producto maestro
 * @param mpn           Numero de parte del fabricante (SKU canonico)
 * @param month         Mes en formato "YYYY-MM"
 * @param avgPrice      Precio promedio del mes actual (en moneda base EUR)
 * @param prevAvgPrice  Precio promedio del mes anterior (null si es el primer registro)
 * @param variationPct  Variacion porcentual respecto al mes anterior (null si no hay historial previo)
 * @param isOutlier     true si |variationPct| supera el umbral de outlier configurado (default 50%)
 */
public record PriceIndexVariationDto(
    Integer supplierId,
    String supplierName,
    Integer productId,
    String mpn,
    String month,
    BigDecimal avgPrice,
    BigDecimal prevAvgPrice,
    BigDecimal variationPct,
    boolean isOutlier
) {}
