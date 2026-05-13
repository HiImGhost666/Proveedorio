package com.proveedorio.backend.dtos.analytics;

/**
 * DTO de tasa de ruptura de stock (Stockout Rate) por proveedor.
 *
 * Un stock de 0 no es solo falta de producto: es perdida de oportunidad de venta.
 * Esta metrica mide que fraccion de los SKUs de cada proveedor reportan stock = 0,
 * permitiendo identificar proveedores con problemas de disponibilidad cronica.
 *
 * @param supplierId      ID del proveedor
 * @param supplierName    Nombre del proveedor
 * @param totalSkus       Total de SKUs asociados al proveedor
 * @param outOfStockSkus  SKUs con stock = 0 o NULL
 * @param stockoutRate    Porcentaje de SKUs sin stock (0.0 - 100.0)
 */
public record StockoutRateDto(
    Integer supplierId,
    String supplierName,
    long totalSkus,
    long outOfStockSkus,
    double stockoutRate
) {}
