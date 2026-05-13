package com.laberit.backend.dtos.product;

import jakarta.validation.constraints.AssertTrue;

import java.math.BigDecimal;

/**
 * Parámetros de búsqueda y paginación para el listado de productos.
 * Usado por el controlador de productos y por {@link com.laberit.backend.service.StockServiceImpl#searchProducts}.
 */
public record ProductSearchRequest(
    String query,
    String category,
    Integer supplierId,
    BigDecimal minPrice,
    BigDecimal maxPrice,
    Boolean availableOnly,
    String productCategory,
    String specsFilter,
    String sortBy,
    int page,
    int size
) {
    public ProductSearchRequest {
        if (sortBy == null) sortBy = "model";
        if (availableOnly == null) availableOnly = false;
    }

    @AssertTrue(message = "El precio máximo debe ser mayor o igual al precio mínimo")
    public boolean isPriceRangeValid() {
        if (minPrice == null || maxPrice == null) return true;
        return maxPrice.compareTo(minPrice) >= 0;
    }
}
