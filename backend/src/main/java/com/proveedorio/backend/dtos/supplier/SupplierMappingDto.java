package com.proveedorio.backend.dtos.supplier;

/**
 * DTO de una regla de mapeo campo externo → campo interno para un proveedor.
 * Usado en GET/POST/PUT de mapeos de proveedor.
 */
public record SupplierMappingDto(
    Integer mappingId,
    Integer supplierId,
    String internalField,
    String externalField,
    String transformationType
) {}
