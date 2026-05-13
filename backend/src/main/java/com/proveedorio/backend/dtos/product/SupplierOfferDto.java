package com.proveedorio.backend.dtos.product;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * DTO de una oferta de un proveedor para un producto: precio coste, PVP, stock, estado y datos de sincronización.
 * Se usa dentro de {@link ProductDetailDto} en el detalle de producto.
 */
public record SupplierOfferDto(
    Integer supplierId,
    String supplierName,
    BigDecimal costPrice,
    BigDecimal retailPrice,
    Integer stock,
    LocalDateTime lastSyncAt,
    String stockStatus,
    String ean,
    Integer moq,
    String condition
) {}
