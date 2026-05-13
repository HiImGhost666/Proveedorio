package com.proveedorio.backend.dtos.dashboard;

/**
 * DTO de una entrada del historial de sincronización (fecha, éxitos, errores).
 * Usado en el gráfico de área del dashboard.
 */
public record SyncHistoryDto(
    String date,
    long successCount,
    long errorCount
) {}
