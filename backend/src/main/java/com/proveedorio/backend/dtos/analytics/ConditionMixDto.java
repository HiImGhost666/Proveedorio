package com.proveedorio.backend.dtos.analytics;

/**
 * Mezcla de condiciones de producto por proveedor (NEW/REFURBISHED/BOX_DAMAGED/USED).
 */
public record ConditionMixDto(
    Integer supplierId,
    String supplierName,
    long totalSkus,
    long newCount,
    long refurbishedCount,
    long boxDamagedCount,
    long usedCount,
    double newPct,
    double refurbishedPct,
    double boxDamagedPct,
    double usedPct
) {}
