package com.proveedorio.backend.dtos.supplier;

/**
 * DTO reducido de proveedor (id y nombre) para listas desplegables y selección.
 */
public record SupplierSimpleDto(
    Integer id,
    String name
) {}
