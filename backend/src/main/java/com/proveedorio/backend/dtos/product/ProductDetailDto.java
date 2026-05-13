package com.proveedorio.backend.dtos.product;

import java.util.List;
import java.util.Map;

/**
 * DTO de detalle de un producto maestro: datos base más la lista de ofertas por proveedor.
 * Se usa en GET /api/v1/productos/{id}.
 */
public record ProductDetailDto(
    Integer id,
    String mpn,
    String model,
    String brand,
    String category,
    Map<String, String> specifications,
    List<SupplierOfferDto> offers
) {}
