package com.proveedorio.backend.dtos.dashboard;

import java.time.LocalDateTime;

/**
 * DTO de producto obsoleto o "zombie" (sin actualización reciente).
 * Usado en la tabla de limpieza de catálogo del dashboard.
 */
public record StaleProductDto(
    Integer id,
    String mpn,
    String name,
    LocalDateTime lastUpdatedAt,
    long daysWithoutUpdate
) {}
