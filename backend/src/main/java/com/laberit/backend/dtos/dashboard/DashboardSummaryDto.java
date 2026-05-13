package com.laberit.backend.dtos.dashboard;

import com.laberit.backend.dtos.supplier.ProviderStatusDto;

import java.util.List;

/**
 * DTO principal del resumen del dashboard: KPIs, datos de gráficos y tablas.
 * Usado en GET /api/v1/dashboard/summary.
 */
public record DashboardSummaryDto(
    long totalProducts,
    long totalSuppliers,
    long productsAvailable,
    long productsLowStock,
    long productsOutOfStock,
    double avgLatencyOverall,

    List<ChartDataDto> productsByBrand,
    List<ChartDataDto> stockBySupplier,
    List<SyncHistoryDto> syncHistory,
    List<TopProductDto> topExpensiveProducts,
    List<TopMoverDto> topPriceIncreases,
    List<TopMoverDto> topPriceDecreases,
    List<StaleProductDto> staleProducts,
    List<ProviderStatusDto> failedProviders
) {}
