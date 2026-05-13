package com.proveedorio.backend.dtos.supplier;

/**
 * DTO de respuesta con datos de un proveedor (listado y detalle).
 * Usado en GET /proveedores y en respuestas de creación/actualización.
 */
public record SupplierResponseDto(
    Integer id,
    String name,
    String baseUrlApi,
    String contact,
    String email,
    String schedule,
    String phone,
    String website,
    String country,
    String defaultCurrency,
    boolean active,
    boolean supportsBulkSync,
    String catalogEndpoint,
    String detailEndpoint,
    String searchEndpoint
) {}
