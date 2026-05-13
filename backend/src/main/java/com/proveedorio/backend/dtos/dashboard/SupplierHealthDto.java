package com.proveedorio.backend.dtos.dashboard;

/**
 * KPIs operativos por proveedor para la seccion "Salud de Proveedores" del Dashboard.
 * SLA Score (0-100): score compuesto = (successRate*0.4) + (latencyScore*0.3) + ((100-stockoutRate)*0.3)
 * latencyScore = MAX(0, 100 - avgLatencyMs/1000)
 */
public record SupplierHealthDto(
    Integer supplierId,
    String supplierName,
    double slaScore,
    double syncSuccessRate,
    double avgItemsPerSync,
    Long avgLatencyMs,
    double apiErrorRate,
    double staleRate,
    String slaGrade
) {}
