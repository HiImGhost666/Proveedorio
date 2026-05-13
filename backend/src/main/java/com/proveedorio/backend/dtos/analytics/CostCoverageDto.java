package com.proveedorio.backend.dtos.analytics;

/**
 * Ratio de cobertura de proveedores por SKU.
 * Evalua el riesgo de dependencia de proveedor unico.
 * SIN_STOCK: 0 proveedores con stock > 0
 * RIESGO_PROVEEDOR_UNICO: exactamente 1 proveedor con stock > 0
 * CUBIERTO: 2+ proveedores con stock > 0
 */
public record CostCoverageDto(
    Integer productId,
    String mpn,
    String category,
    int totalSuppliers,
    int suppliersWithStock,
    String coverageStatus
) {}
