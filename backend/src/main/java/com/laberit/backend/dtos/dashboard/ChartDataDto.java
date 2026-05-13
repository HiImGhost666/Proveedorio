package com.laberit.backend.dtos.dashboard;

/**
 * DTO para datos de gráficos (etiqueta + valor). Usado en gráficos de pie y barras del dashboard.
 */
public record ChartDataDto(
    String label,
    Number value
) {}
