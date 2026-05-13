package com.laberit.backend.service;

import com.laberit.backend.dtos.SyncStatus;
import com.laberit.backend.dtos.product.AggregatedProductDto;
import com.laberit.backend.dtos.product.ProductDetailDto;
import com.laberit.backend.dtos.product.ProductSearchRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface IStockService {

    /**
     * Busca productos y devuelve una vista agregada para tablas.
     */
    Page<AggregatedProductDto> searchProducts(ProductSearchRequest request, Pageable pageable);

    /**
     * Obtiene todos los productos en una vista agregada para tablas.
     */
    Page<AggregatedProductDto> findAllProducts(Pageable pageable);

    /**
     * Obtiene la vista detallada de un único producto, incluyendo todas sus ofertas.
     */
    Optional<ProductDetailDto> findProductDetailsById(Integer id);

    /**
     * Dispara el proceso de sincronización masiva.
     */
    void syncAllProducts();

    /**
     * Obtiene el estado de la sincronización.
     */
    SyncStatus getSyncStatus();
}

