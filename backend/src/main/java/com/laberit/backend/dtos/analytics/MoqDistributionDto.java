package com.laberit.backend.dtos.analytics;

/**
 * Distribucion de MOQ (Minimum Order Quantity) por proveedor.
 * MOQ alto = barrera de compra.
 */
public record MoqDistributionDto(
    Integer supplierId,
    String supplierName,
    double avgMoq,
    int maxMoq,
    int minMoq,
    long skusWithMoqAbove10
) {}
