package com.proveedorio.backend.dtos.supplier;

/**
 * DTO con métricas de dashboard de un proveedor: total productos, stock, latencia.
 * Usado en GET /proveedores/{id}/dashboard.
 */
public record SupplierDashboardDto(
    Integer supplierId,
    String supplierName,
    long totalProducts,
    long productsInStock,
    long productsOutOfStock,
    double avgSyncLatencyHours
) {}
