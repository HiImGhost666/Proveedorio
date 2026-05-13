package com.proveedorio.backend.service.analytics;

import com.proveedorio.backend.dtos.analytics.*;
import com.proveedorio.backend.entity.Supplier;
import com.proveedorio.backend.repository.*;
import com.proveedorio.backend.repository.PriceHistoryRepository;
import com.proveedorio.backend.repository.ProductSupplierRepository;
import com.proveedorio.backend.repository.SupplierRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Servicio de analytics de Supply Chain.
 *
 * Implementa los tres KPIs avanzados definidos en el contexto de negocio:
 *   1. Price Index Variation — variacion mensual de costes por proveedor (LAG).
 *   2. Stockout Rate         — tasa de ruptura de stock por proveedor.
 *   3. ML Dataset            — vista desnormalizada para modelos de prediccion.
 *
 * Regla de normalizacion de monedas:
 *   Antes de agregar KPIs de precios, la query de PriceIndexVariation aplica
 *   la tabla currency_rates para convertir a EUR. Si no existe tasa registrada
 *   para un par de monedas, se asume factor 1.0 (misma moneda).
 *
 * Regla de deduplicacion:
 *   Los KPIs operan sobre MasterProduct.mpn como SKU canonico. Un mismo MPN
 *   de diferentes proveedores se trata como una sola entidad en el inventario global.
 */
@Service
public class SupplierAnalyticsService {

    private static final Logger log = LoggerFactory.getLogger(SupplierAnalyticsService.class);

    private static final int DEFAULT_MONTHS        = 6;
    private static final double DEFAULT_OUTLIER_THRESHOLD = 50.0;
    private static final int DEFAULT_ML_MAX_ROWS   = 1000;

    private final PriceHistoryRepository    priceHistoryRepository;
    private final ProductSupplierRepository productSupplierRepository;
    private final SupplierRepository        supplierRepository;
    private final MasterProductRepository   masterProductRepository;
    private final StockHistoryRepository    stockHistoryRepository;

    public SupplierAnalyticsService(
            PriceHistoryRepository priceHistoryRepository,
            ProductSupplierRepository productSupplierRepository,
            SupplierRepository supplierRepository,
            MasterProductRepository masterProductRepository,
            StockHistoryRepository stockHistoryRepository) {
        this.priceHistoryRepository    = priceHistoryRepository;
        this.productSupplierRepository = productSupplierRepository;
        this.supplierRepository        = supplierRepository;
        this.masterProductRepository   = masterProductRepository;
        this.stockHistoryRepository    = stockHistoryRepository;
    }

    // =========================================================================
    // KPI A — Price Index Variation
    // =========================================================================

    /**
     * Calcula la variacion mensual del indice de precios por proveedor y SKU.
     *
     * Usa window function LAG() sobre historial_precios para comparar el precio
     * promedio mensual con el del mes anterior. Los registros cuya variacion
     * supera el umbral de outlier se marcan como isOutlier=true pero se incluyen
     * en la respuesta para que el frontend pueda filtrarlos visualmente.
     *
     * @param months           ventana de analisis en meses (default 6)
     * @param outlierThreshold umbral de variacion absoluta para clasificar outliers (default 50.0%)
     * @return lista de variaciones mensuales ordenada por proveedor, producto y mes
     */
    @Transactional(readOnly = true)
    public List<PriceIndexVariationDto> getPriceIndexVariation(int months, double outlierThreshold) {
        log.info("Calculando Price Index Variation: {} meses, umbral outlier {}%", months, outlierThreshold);

        List<Object[]> rows;
        try {
            rows = priceHistoryRepository.findPriceIndexVariation(months, outlierThreshold);
        } catch (DataAccessException ex) {
            // Fallback defensivo para entornos con tabla currency_rates dañada o ausente.
            log.warn("No se pudo aplicar normalizacion por currency_rates; usando fallback sin conversion de divisa. Causa: {}",
                ex.getMostSpecificCause() != null ? ex.getMostSpecificCause().getMessage() : ex.getMessage());
            rows = priceHistoryRepository.findPriceIndexVariationWithoutCurrencyRates(months, outlierThreshold);
        }

        List<PriceIndexVariationDto> result = new ArrayList<>(rows.size());

        for (Object[] row : rows) {
            BigDecimal variationPct = row[7] != null ? new BigDecimal(row[7].toString()) : null;
            boolean isOutlier = variationPct != null
                && variationPct.abs().doubleValue() > outlierThreshold;

            result.add(new PriceIndexVariationDto(
                toInt(row[0]),               // supplier_id
                str(row[1]),                 // supplier_name
                toInt(row[2]),               // product_id
                str(row[3]),                 // mpn
                str(row[4]),                 // month
                toBigDecimal(row[5]),        // avg_price
                toBigDecimal(row[6]),        // prev_avg_price
                variationPct,               // variation_pct
                isOutlier
            ));
        }

        log.info("Price Index Variation: {} registros generados", result.size());
        return result;
    }

    // =========================================================================
    // KPI B — Stockout Rate
    // =========================================================================

    /**
     * Calcula la tasa de ruptura de stock (Stockout Rate) para cada proveedor activo.
     *
     * Stockout Rate = (SKUs con stock = 0) / (total SKUs del proveedor) * 100
     *
     * Un proveedor con stockout rate > 30% indica problemas sistemicos de disponibilidad.
     * Los datos provienen de ProductSupplier.stock (valor actual; no es historico).
     *
     * @return lista de StockoutRateDto ordenada por stockoutRate descendente
     */
    @Transactional(readOnly = true)
    public List<StockoutRateDto> getStockoutRates() {
        log.info("Calculando Stockout Rates por proveedor");

        List<Supplier> suppliers = supplierRepository.findAll();
        List<StockoutRateDto> result = new ArrayList<>(suppliers.size());

        for (Supplier supplier : suppliers) {
            long total       = productSupplierRepository.countBySupplierId(supplier.getId());
            long outOfStock  = productSupplierRepository.countBySupplierIdAndStockIsZeroOrNull(supplier.getId());
            double rate      = total > 0 ? (outOfStock * 100.0) / total : 0.0;

            result.add(new StockoutRateDto(
                supplier.getId(),
                supplier.getName(),
                total,
                outOfStock,
                Math.round(rate * 100.0) / 100.0
            ));
        }

        result.sort((a, b) -> Double.compare(b.stockoutRate(), a.stockoutRate()));

        log.info("Stockout Rates: {} proveedores procesados", result.size());
        return result;
    }

    // ML dataset generation removed from service

    // =========================================================================
    // KPIs nuevos — alta y media viabilidad
    // =========================================================================

    @Transactional(readOnly = true)
    public List<PriceDispersionDto> getPriceDispersion(int maxRows) {
        log.info("Calculando Price Dispersion Index (maxRows={})", maxRows);
        List<Object[]> rows = productSupplierRepository.findPriceDispersionIndex(maxRows);
        List<PriceDispersionDto> result = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            result.add(new PriceDispersionDto(
                toInt(row[0]),          // product_id
                str(row[1]),            // mpn
                str(row[2]),            // category
                toInt(row[3]),          // supplier_count
                toBigDecimal(row[4]),   // min_price
                toBigDecimal(row[5]),   // max_price
                toBigDecimal(row[6]),   // avg_price
                toDouble(row[7])        // dispersion_pct
            ));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<MoqDistributionDto> getMoqDistribution() {
        log.info("Calculando MOQ Distribution");
        List<Object[]> rows = productSupplierRepository.findMoqDistributionPerSupplier();
        List<MoqDistributionDto> result = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            result.add(new MoqDistributionDto(
                toInt(row[0]),    // supplier_id
                str(row[1]),      // supplier_name
                toDouble(row[2]), // avg_moq
                toInt(row[3]),    // max_moq
                toInt(row[4]),    // min_moq
                toLong(row[5])    // skus_moq_above_10
            ));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<ConditionMixDto> getConditionMix() {
        log.info("Calculando Condition Mix");
        List<Object[]> rows = productSupplierRepository.findConditionMixPerSupplier();
        List<ConditionMixDto> result = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            long total      = toLongPrimitive(row[2]);
            long newC       = toLongPrimitive(row[3]);
            long refurb     = toLongPrimitive(row[4]);
            long boxDmg     = toLongPrimitive(row[5]);
            long used       = toLongPrimitive(row[6]);
            result.add(new ConditionMixDto(
                toInt(row[0]),                                       // supplier_id
                str(row[1]),                                         // supplier_name
                total,
                newC, refurb, boxDmg, used,
                total > 0 ? Math.round(newC   * 1000.0 / total) / 10.0 : 0,
                total > 0 ? Math.round(refurb * 1000.0 / total) / 10.0 : 0,
                total > 0 ? Math.round(boxDmg * 1000.0 / total) / 10.0 : 0,
                total > 0 ? Math.round(used   * 1000.0 / total) / 10.0 : 0
            ));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<CostCoverageDto> getCostCoverage(int maxRows) {
        log.info("Calculando Cost Coverage Ratio (maxRows={})", maxRows);
        List<Object[]> rows = productSupplierRepository.findCostCoverageRatio(maxRows);
        List<CostCoverageDto> result = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            result.add(new CostCoverageDto(
                toInt(row[0]),  // product_id
                str(row[1]),    // mpn
                str(row[2]),    // category
                toInt(row[3]),  // total_suppliers
                toInt(row[4]),  // suppliers_with_stock
                str(row[5])     // coverage_status
            ));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<StockVolatilityDto> getStockVolatility(int days, int maxRows) {
        log.info("Calculando Stock Volatility Index (days={}, maxRows={})", days, maxRows);
        List<Object[]> rows = stockHistoryRepository.findStockVolatility(days, maxRows);
        List<StockVolatilityDto> result = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            result.add(new StockVolatilityDto(
                toInt(row[0]),   // supplier_id
                str(row[1]),     // supplier_name
                toInt(row[2]),   // product_id
                str(row[3]),     // mpn
                toInt(row[4]),   // changes_in_24h
                toInt(row[5]),   // max_stock
                toInt(row[6]),   // min_stock
                str(row[7])      // volatility_type
            ));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<CatalogGrowthDto> getCatalogGrowth(int weeks) {
        log.info("Calculando Catalog Growth Trend (weeks={})", weeks);
        List<Object[]> rows = masterProductRepository.findCatalogGrowthPerWeek(weeks);
        List<CatalogGrowthDto> result = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            int year = toInt(row[0]);
            int week = toInt(row[1]);
            long count = toLongPrimitive(row[2]);
            result.add(new CatalogGrowthDto(year, week, count,
                    String.format("%d-W%02d", year, week)));
        }
        return result;
    }

    @Transactional(readOnly = true)
    public List<PriceStabilityDto> getPriceStability(int days, int maxRows) {
        log.info("Calculando Price Stability Score (days={}, maxRows={})", days, maxRows);
        List<Object[]> rows = priceHistoryRepository.findPriceStabilityScores(days, maxRows);
        List<PriceStabilityDto> result = new ArrayList<>(rows.size());
        for (Object[] row : rows) {
            result.add(new PriceStabilityDto(
                toInt(row[0]),          // product_id
                str(row[1]),            // mpn
                toInt(row[2]),          // supplier_id
                str(row[3]),            // supplier_name
                toBigDecimal(row[4]),   // avg_price
                toBigDecimal(row[5]),   // stddev_price
                toDouble(row[6]),       // cv_pct
                str(row[7]),            // stability_label
                toInt(row[8])           // price_points
            ));
        }
        return result;
    }

    // =========================================================================
    // Helpers de conversion de tipos para Object[] nativo
    // =========================================================================

    private Integer toInt(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.intValue();
        try { return Integer.parseInt(v.toString()); } catch (NumberFormatException e) { return null; }
    }

    private Long toLong(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.longValue();
        try { return Long.parseLong(v.toString()); } catch (NumberFormatException e) { return null; }
    }

    private BigDecimal toBigDecimal(Object v) {
        if (v == null) return null;
        if (v instanceof BigDecimal bd) return bd;
        try { return new BigDecimal(v.toString()); } catch (NumberFormatException e) { return null; }
    }

    private String str(Object v) {
        return v != null ? v.toString() : null;
    }

    private boolean toBoolean(Object v) {
        if (v == null) return false;
        if (v instanceof Boolean b) return b;
        if (v instanceof Number n) return n.intValue() == 1;
        return "1".equals(v.toString()) || "true".equalsIgnoreCase(v.toString());
    }

    private LocalDateTime toLocalDateTime(Object v) {
        if (v == null) return null;
        if (v instanceof LocalDateTime ldt) return ldt;
        return null;
    }

    private double toDouble(Object v) {
        if (v == null) return 0.0;
        if (v instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(v.toString()); } catch (NumberFormatException e) { return 0.0; }
    }

    private long toLongPrimitive(Object v) {
        Long l = toLong(v);
        return l != null ? l : 0L;
    }
}
