package com.proveedorio.backend.controller;

import com.proveedorio.backend.dtos.analytics.*;
import com.proveedorio.backend.service.analytics.SupplierAnalyticsService;
import com.proveedorio.backend.util.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controlador REST para los KPIs avanzados de Supply Chain Analytics.
 *
 * Todos los endpoints requieren autenticacion (ADMIN o CLIENTE).
 * La ruta base es /api/v1/analytics.
 *
 * Endpoints disponibles:
 *   GET /price-variation   — variacion mensual de indices de precio (LAG)
 *   GET /stockout-rates    — tasa de ruptura de stock por proveedor
 *   GET /price-dispersion  — dispersion de precio entre proveedores por SKU
 *   GET /moq               — distribucion de MOQ por proveedor
 *   GET /condition-mix     — mezcla de condicion de producto por proveedor
 *   GET /cost-coverage     — cobertura de proveedor por SKU (riesgo proveedor unico)
 *   GET /stock-volatility  — indice de volatilidad de stock (24h)
 *   GET /catalog-growth    — tendencia de crecimiento del catalogo (semanal)
 *   GET /price-stability   — score de estabilidad de precio por SKU/proveedor
 */
@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsController.class);

    private final SupplierAnalyticsService analyticsService;

    public AnalyticsController(SupplierAnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
    }

    /**
     * Devuelve la variacion mensual del indice de precios por proveedor y SKU.
     *
     * Usa window function LAG() sobre historial_precios agrupado por mes.
     * Los precios se normalizan a EUR usando currency_rates.
     *
     * @param months           numero de meses a analizar (default 6)
     * @param outlierThreshold variacion porcentual maxima antes de marcar como outlier (default 50.0)
     * @return lista de PriceIndexVariationDto ordenada por proveedor, SKU y mes
     */
    @GetMapping("/price-variation")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<PriceIndexVariationDto>>> getPriceVariation(
            @RequestParam(defaultValue = "6")    int    months,
            @RequestParam(defaultValue = "50.0") double outlierThreshold) {

        log.info("GET /api/v1/analytics/price-variation months={} outlierThreshold={}", months, outlierThreshold);
        List<PriceIndexVariationDto> data;
        ApiResponse<List<PriceIndexVariationDto>> response = new ApiResponse<>();
        try {
            data = analyticsService.getPriceIndexVariation(months, outlierThreshold);
            response.setSuccess(true);
            response.setMessage("Price Index Variation calculado correctamente");
            response.setData(data);
        } catch (Exception ex) {
            log.error("Error al calcular Price Index Variation: {}", ex.getMessage(), ex);
            response.setSuccess(false);
            response.setMessage("No se pudo calcular Price Index Variation: " + ex.getMessage());
            response.setData(List.of());
            response.getMetadata().put("exception", ex.getClass().getSimpleName());
            response.getMetadata().put("reason", ex.getMessage());
        }
        return ResponseEntity.ok(response);
    }

    /**
     * Devuelve la tasa de ruptura de stock (Stockout Rate) por proveedor.
     *
     * Calcula: (SKUs con stock = 0) / total SKUs * 100 para cada proveedor activo.
     * Ordena por stockoutRate descendente para destacar los proveedores mas criticos.
     *
     * @return lista de StockoutRateDto ordenada por tasa de ruptura descendente
     */
    @GetMapping("/stockout-rates")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<StockoutRateDto>>> getStockoutRates() {

        log.info("GET /api/v1/analytics/stockout-rates");
        List<StockoutRateDto> data = analyticsService.getStockoutRates();

        ApiResponse<List<StockoutRateDto>> response = new ApiResponse<>();
        response.setSuccess(true);
        response.setMessage("Stockout Rates calculados correctamente");
        response.setData(data);
        return ResponseEntity.ok(response);
    }

    // ML dataset endpoint removed

    @GetMapping("/price-dispersion")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<PriceDispersionDto>>> getPriceDispersion(
            @RequestParam(defaultValue = "100") int maxRows) {
        log.info("GET /api/v1/analytics/price-dispersion maxRows={}", maxRows);
        List<PriceDispersionDto> data = analyticsService.getPriceDispersion(maxRows);
        ApiResponse<List<PriceDispersionDto>> resp = new ApiResponse<>();
        resp.setSuccess(true);
        resp.setMessage("Price Dispersion Index calculado correctamente");
        resp.setData(data);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/moq")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<MoqDistributionDto>>> getMoqDistribution() {
        log.info("GET /api/v1/analytics/moq");
        List<MoqDistributionDto> data = analyticsService.getMoqDistribution();
        ApiResponse<List<MoqDistributionDto>> resp = new ApiResponse<>();
        resp.setSuccess(true);
        resp.setMessage("MOQ Distribution calculado correctamente");
        resp.setData(data);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/condition-mix")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<ConditionMixDto>>> getConditionMix() {
        log.info("GET /api/v1/analytics/condition-mix");
        List<ConditionMixDto> data = analyticsService.getConditionMix();
        ApiResponse<List<ConditionMixDto>> resp = new ApiResponse<>();
        resp.setSuccess(true);
        resp.setMessage("Condition Mix calculado correctamente");
        resp.setData(data);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/cost-coverage")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<CostCoverageDto>>> getCostCoverage(
            @RequestParam(defaultValue = "200") int maxRows) {
        log.info("GET /api/v1/analytics/cost-coverage maxRows={}", maxRows);
        List<CostCoverageDto> data = analyticsService.getCostCoverage(maxRows);
        ApiResponse<List<CostCoverageDto>> resp = new ApiResponse<>();
        resp.setSuccess(true);
        resp.setMessage("Cost Coverage Ratio calculado correctamente");
        resp.setData(data);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/stock-volatility")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<StockVolatilityDto>>> getStockVolatility(
            @RequestParam(defaultValue = "30") int days,
            @RequestParam(defaultValue = "100") int maxRows) {
        log.info("GET /api/v1/analytics/stock-volatility days={} maxRows={}", days, maxRows);
        List<StockVolatilityDto> data = analyticsService.getStockVolatility(days, maxRows);
        ApiResponse<List<StockVolatilityDto>> resp = new ApiResponse<>();
        resp.setSuccess(true);
        resp.setMessage("Stock Volatility Index calculado correctamente");
        resp.setData(data);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/catalog-growth")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<CatalogGrowthDto>>> getCatalogGrowth(
            @RequestParam(defaultValue = "12") int weeks) {
        log.info("GET /api/v1/analytics/catalog-growth weeks={}", weeks);
        List<CatalogGrowthDto> data = analyticsService.getCatalogGrowth(weeks);
        ApiResponse<List<CatalogGrowthDto>> resp = new ApiResponse<>();
        resp.setSuccess(true);
        resp.setMessage("Catalog Growth Trend calculado correctamente");
        resp.setData(data);
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/price-stability")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ApiResponse<List<PriceStabilityDto>>> getPriceStability(
            @RequestParam(defaultValue = "90") int days,
            @RequestParam(defaultValue = "100") int maxRows) {
        log.info("GET /api/v1/analytics/price-stability days={} maxRows={}", days, maxRows);
        List<PriceStabilityDto> data = analyticsService.getPriceStability(days, maxRows);
        ApiResponse<List<PriceStabilityDto>> resp = new ApiResponse<>();
        resp.setSuccess(true);
        resp.setMessage("Price Stability Score calculado correctamente");
        resp.setData(data);
        return ResponseEntity.ok(resp);
    }
}
