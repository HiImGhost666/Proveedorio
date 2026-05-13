package com.laberit.backend.controller;

import com.laberit.backend.dtos.pricing.PriceHistoryItemDto;
import com.laberit.backend.dtos.product.AggregatedProductDto;
import com.laberit.backend.dtos.product.ProductDetailDto;
import com.laberit.backend.dtos.product.ProductSearchRequest;
import com.laberit.backend.service.PriceHistoryService;
import com.laberit.backend.service.IStockService;
import com.laberit.backend.util.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controlador REST del inventario consolidado de productos.
 * Expone listado paginado, búsqueda con filtros, detalle de producto, historial de precios por proveedor
 * y disparo de sincronización masiva. Utiliza {@link IStockService} y {@link PriceHistoryService}.
 */
@RestController
@RequestMapping("/api/v1/productos")
public class ProductController {

    private static final Logger logger = LoggerFactory.getLogger(ProductController.class);
    private final IStockService stockService;
    private final PriceHistoryService priceHistoryService;

    public ProductController(IStockService stockService, PriceHistoryService priceHistoryService) {
        this.stockService = stockService;
        this.priceHistoryService = priceHistoryService;
    }

    /**
     * Devuelve la primera página de productos del inventario consolidado con orden y tamaño configurables.
     * No aplica filtros de búsqueda; para filtrar se usa el endpoint de búsqueda.
     *
     * @param page     índice de página (0-based).
     * @param size     tamaño de página.
     * @param sortBy   campo por el que ordenar (p. ej. model, brand, precioMinimo).
     * @param direction ASC o DESC.
     * @return página de {@link AggregatedProductDto} y mensaje de éxito.
     */
    @GetMapping
    public ResponseEntity<ApiResponse<Page<AggregatedProductDto>>> getAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "model") String sortBy,
            @RequestParam(defaultValue = "ASC") Sort.Direction direction
    ) {
        logger.info("GET /api/v1/productos - page={}, size={}, sort={}, dir={}", page, size, sortBy, direction);
        ProductSearchRequest request = new ProductSearchRequest(
                null,  // query
                null,  // category (brand filter)
                null,  // supplierId
                null,  // minPrice
                null,  // maxPrice
                null,  // availableOnly
                null,  // productCategory (nueva categoría funcional)
                null,  // specsFilter
                sortBy,
                page,
                size
        );
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<AggregatedProductDto> result = stockService.searchProducts(request, pageable);

        return ResponseEntity.ok(ApiResponse.success(result, "Productos obtenidos con éxito"));
    }

    /**
     * Busca productos aplicando filtros opcionales (texto, categoría, proveedor, especificaciones)
     * y devuelve una página de resultados ordenada.
     *
     * @param query      texto para buscar en modelo, marca o MPN.
     * @param categoria  filtro por marca (brand).
     * @param proveedorId filtro por proveedor.
     * @param specsFilter texto a buscar dentro de las especificaciones técnicas.
     * @param page       índice de página.
     * @param size       tamaño de página.
     * @param sortBy     campo de ordenación.
     * @param direction  dirección del orden.
     * @return página de {@link AggregatedProductDto} con los resultados.
     */
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<Page<AggregatedProductDto>>> searchProducts(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String categoria,
            @RequestParam(required = false) String productCategory,
            @RequestParam(required = false) Integer proveedorId,
            @RequestParam(required = false) String specsFilter,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "model") String sortBy,
            @RequestParam(defaultValue = "ASC") Sort.Direction direction
    ) {
        logger.info("GET /api/v1/productos/search - query='{}', brandFilter='{}', productCategory='{}', provId={}, specsFilter='{}'",
                query, categoria, productCategory, proveedorId, specsFilter);
        ProductSearchRequest request = new ProductSearchRequest(query, categoria, proveedorId, null, null, null, productCategory, specsFilter, sortBy, page, size);
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));

        Page<AggregatedProductDto> result = stockService.searchProducts(request, pageable);

        return ResponseEntity.ok(ApiResponse.success(result, "Búsqueda completada con éxito"));
    }

    /**
     * Obtiene el detalle de un producto por ID: datos del producto maestro y lista de ofertas por proveedor.
     * Responde 404 si el producto no existe.
     *
     * @param id identificador del producto maestro.
     * @return detalle con ofertas o error 404.
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<ProductDetailDto>> getProductDetails(@PathVariable Integer id) {
        logger.info("GET /api/v1/productos/{}", id);
        return stockService.findProductDetailsById(id)
                .map(dto -> ResponseEntity.ok(ApiResponse.success(dto, "Detalle de producto obtenido")))
                .orElse(ResponseEntity.status(404).body(ApiResponse.error("Producto no encontrado")));
    }

    /**
     * Devuelve el historial de precios registrado para un producto con un proveedor concreto.
     * Usado por la vista de análisis de precios en el frontend.
     *
     * @param id          ID del producto maestro.
     * @param proveedorId ID del proveedor.
     * @return lista de registros de historial con precio, fecha y tendencia.
     */
    @GetMapping("/{id}/historial/{proveedorId}")
    public ResponseEntity<ApiResponse<List<PriceHistoryItemDto>>> getHistorialPrecios(
            @PathVariable Integer id,
            @PathVariable Integer proveedorId
    ) {
        logger.info("GET /api/v1/productos/{}/historial/{}", id, proveedorId);
        List<PriceHistoryItemDto> historial = priceHistoryService.getHistoryDto(id, proveedorId);
        return ResponseEntity.ok(ApiResponse.success(historial, "Historial de precios obtenido"));
    }

    /**
     * Inicia la sincronización masiva de catálogos en segundo plano.
     * Responde de inmediato con 202; el proceso se ejecuta de forma asíncrona.
     */
    @PostMapping("/sync-all")
    public ResponseEntity<ApiResponse<Void>> syncAllProducts() {
        logger.info("POST /api/v1/productos/sync-all - Iniciando sincronización manual");
        stockService.syncAllProducts();
        return ResponseEntity.accepted().body(ApiResponse.success(null, "Sincronización masiva iniciada en segundo plano"));
    }

    /**
     * Devuelve el estado actual de la sincronización (IDLE, IN_PROGRESS, COMPLETED, ERROR).
     */
    @GetMapping("/sync-status")
    public ResponseEntity<ApiResponse<Map<String, String>>> getSyncStatus() {
        Map<String, String> response = new HashMap<>();
        response.put("status", stockService.getSyncStatus().name());
        return ResponseEntity.ok(ApiResponse.success(response, "Estado de sincronización obtenido"));
    }
}
