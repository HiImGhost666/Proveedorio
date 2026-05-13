package com.proveedorio.backend.dtos.analytics;

/**
 * Tendencia de crecimiento del catalogo de productos nuevos por semana.
 */
public record CatalogGrowthDto(
    int year,
    int week,
    long newProducts,
    String weekLabel
) {}
