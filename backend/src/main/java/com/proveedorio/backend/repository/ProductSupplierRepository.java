package com.proveedorio.backend.repository;

import com.proveedorio.backend.dtos.dashboard.ChartDataDto;
import com.proveedorio.backend.dtos.dashboard.TopProductDto;
import com.proveedorio.backend.entity.ProductSupplier;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ProductSupplierRepository extends JpaRepository<ProductSupplier, Integer> {
    
    List<ProductSupplier> findByMasterProductId(Integer masterProductId);

    Optional<ProductSupplier> findByMasterProductIdAndSupplierId(Integer masterProductId, Integer supplierId);

    Optional<ProductSupplier> findBySupplierIdAndExternalProviderId(Integer supplierId, String externalProviderId);

    long countBySupplierId(Integer supplierId);

    @Query("SELECT COUNT(ps) FROM ProductSupplier ps WHERE ps.supplier.id = :supplierId AND (ps.stock <= 0 OR ps.stock IS NULL)")
    long countBySupplierIdAndStockIsZeroOrNull(@Param("supplierId") Integer supplierId);

    /**
     * Agrega el stock total por proveedor para alimentar el gráfico
     * de "Stock por proveedor" del dashboard.
     * Solo se consideran filas con stock no nulo.
     */
    @Query("SELECT new com.proveedorio.backend.dtos.dashboard.ChartDataDto(ps.supplier.name, SUM(ps.stock)) " +
           "FROM ProductSupplier ps " +
           "WHERE ps.stock IS NOT NULL " +
           "GROUP BY ps.supplier.name " +
           "ORDER BY SUM(ps.stock) DESC")
    List<ChartDataDto> findStockByProvider(Pageable pageable);

    /**
     * Obtiene los productos más caros combinando marca y modelo en un solo nombre,
     * junto con el proveedor, el precio asociado y el id del producto maestro.
     * Usado en el widget de "Top productos caros" del dashboard.
     */
    @Query("SELECT new com.proveedorio.backend.dtos.dashboard.TopProductDto(" +
           "ps.masterProduct.id, " +
           "CONCAT(ps.masterProduct.brand, ' ', ps.masterProduct.model), " +
           "ps.supplier.name, " +
           "ps.price) " +
           "FROM ProductSupplier ps " +
           "WHERE ps.price IS NOT NULL " +
           "ORDER BY ps.price DESC")
    List<TopProductDto> findTopExpensiveProducts(Pageable pageable);

    /**
     * Genera el dataset desnormalizado para modelos de Machine Learning de prediccion
     * de reabastecimiento.
     *
     * Columnas devueltas (Object[]):
     *   [0] last_updated    (LocalDateTime) - marca de tiempo del ultimo sync del SKU
     *   [1] product_id      (Integer)
     *   [2] mpn             (String)
     *   [3] category        (String)
     *   [4] supplier_id     (Integer)
     *   [5] supplier_name   (String)
     *   [6] currency        (String)
     *   [7] price           (BigDecimal) - precio de coste
     *   [8] retail_price    (BigDecimal) - PVP recomendado, puede ser null
     *   [9] stock           (Integer)
     *   [10] is_promotion   (boolean) - price &lt; retail_price * 0.85
     *   [11] lead_time_ms   (Long) - latencia del ultimo sync del proveedor
     *
     * Lead time: latencia del ciclo de sync mas reciente del proveedor (SyncLog.latencyMs).
     * is_promotion: precio de coste al menos 15% por debajo del PVP sugerido.
     */
    /**
     * API Error Rate por proveedor: COUNT(last_error IS NOT NULL) / total * 100.
     * Columnas: supplier_id, supplier_name, total_skus, error_count, error_rate_pct
     */
    @Query(value = """
        SELECT
            s.id                                                   AS supplier_id,
            s.nombre                                               AS supplier_name,
            COUNT(*)                                               AS total_skus,
            SUM(CASE WHEN ps.last_error IS NOT NULL THEN 1 ELSE 0 END) AS error_count,
            ROUND(SUM(CASE WHEN ps.last_error IS NOT NULL THEN 1 ELSE 0 END)
                  / COUNT(*) * 100, 2)                             AS error_rate_pct
        FROM product_supplier ps
        JOIN proveedores s ON s.id = ps.proveedor_id
        GROUP BY s.id, s.nombre
        ORDER BY error_rate_pct DESC
        """, nativeQuery = true)
    List<Object[]> findApiErrorRatePerSupplier();

    /**
     * Stale Rate por proveedor: COUNT(data_stale=true) / total * 100.
     * Columnas: supplier_id, supplier_name, total_skus, stale_count, stale_rate_pct
     */
    @Query(value = """
        SELECT
            s.id                                                   AS supplier_id,
            s.nombre                                               AS supplier_name,
            COUNT(*)                                               AS total_skus,
            SUM(CASE WHEN ps.data_stale = true THEN 1 ELSE 0 END) AS stale_count,
            ROUND(SUM(CASE WHEN ps.data_stale = true THEN 1 ELSE 0 END)
                  / COUNT(*) * 100, 2)                             AS stale_rate_pct
        FROM product_supplier ps
        JOIN proveedores s ON s.id = ps.proveedor_id
        GROUP BY s.id, s.nombre
        ORDER BY stale_rate_pct DESC
        """, nativeQuery = true)
    List<Object[]> findStaleRatePerSupplier();

    /**
     * MOQ Distribution por proveedor: avg/max/min MOQ y SKUs con MOQ > 10.
     * Columnas: supplier_id, supplier_name, avg_moq, max_moq, min_moq, skus_moq_above_10
     */
    @Query(value = """
        SELECT
            s.id                                                    AS supplier_id,
            s.nombre                                                AS supplier_name,
            ROUND(AVG(COALESCE(ps.moq, 1)), 2)                     AS avg_moq,
            MAX(COALESCE(ps.moq, 1))                               AS max_moq,
            MIN(COALESCE(ps.moq, 1))                               AS min_moq,
            SUM(CASE WHEN ps.moq > 10 THEN 1 ELSE 0 END)          AS skus_moq_above_10
        FROM product_supplier ps
        JOIN proveedores s ON s.id = ps.proveedor_id
        GROUP BY s.id, s.nombre
        ORDER BY avg_moq DESC
        """, nativeQuery = true)
    List<Object[]> findMoqDistributionPerSupplier();

    /**
     * Condition Mix por proveedor: conteo por condicion (NEW/REFURBISHED/BOX_DAMAGED/USED).
     * Columnas: supplier_id, supplier_name, total_skus, new_count, refurbished_count,
     *           box_damaged_count, used_count
     */
    @Query(value = """
        SELECT
            s.id                                                            AS supplier_id,
            s.nombre                                                        AS supplier_name,
            COUNT(*)                                                        AS total_skus,
            SUM(CASE WHEN ps.product_condition = 'NEW'          THEN 1 ELSE 0 END) AS new_count,
            SUM(CASE WHEN ps.product_condition = 'REFURBISHED'  THEN 1 ELSE 0 END) AS refurbished_count,
            SUM(CASE WHEN ps.product_condition = 'BOX_DAMAGED'  THEN 1 ELSE 0 END) AS box_damaged_count,
            SUM(CASE WHEN ps.product_condition = 'USED'         THEN 1 ELSE 0 END) AS used_count
        FROM product_supplier ps
        JOIN proveedores s ON s.id = ps.proveedor_id
        GROUP BY s.id, s.nombre
        ORDER BY s.nombre
        """, nativeQuery = true)
    List<Object[]> findConditionMixPerSupplier();

    /**
     * Price Dispersion Index para SKUs disponibles en 2+ proveedores.
     * dispersionPct = (MAX - MIN) / AVG * 100.
     * Columnas: product_id, mpn, category, supplier_count, min_price, max_price, avg_price,
     *           dispersion_pct
     */
    @Query(value = """
        SELECT
            mp.id                                           AS product_id,
            mp.mpn                                         AS mpn,
            mp.category                                    AS category,
            COUNT(DISTINCT ps.proveedor_id)                AS supplier_count,
            MIN(ps.price)                                  AS min_price,
            MAX(ps.price)                                  AS max_price,
            AVG(ps.price)                                  AS avg_price,
            ROUND((MAX(ps.price) - MIN(ps.price)) / AVG(ps.price) * 100, 2) AS dispersion_pct
        FROM product_supplier ps
        JOIN producto_maestro mp ON mp.id = ps.producto_id
        WHERE ps.price IS NOT NULL AND ps.price > 0
        GROUP BY mp.id, mp.mpn, mp.category
        HAVING COUNT(DISTINCT ps.proveedor_id) >= 2
        ORDER BY dispersion_pct DESC
        LIMIT :maxRows
        """, nativeQuery = true)
    List<Object[]> findPriceDispersionIndex(@Param("maxRows") int maxRows);

    /**
     * Cost Coverage Ratio por MPN: cuantos proveedores tienen stock > 0.
     * Clasifica en SIN_STOCK / RIESGO_PROVEEDOR_UNICO / CUBIERTO.
     * Columnas: product_id, mpn, category, total_suppliers, suppliers_with_stock, coverage_status
     */
    @Query(value = """
        SELECT
            mp.id                                                              AS product_id,
            mp.mpn                                                             AS mpn,
            mp.category                                                        AS category,
            COUNT(DISTINCT ps.proveedor_id)                                   AS total_suppliers,
            SUM(CASE WHEN ps.stock > 0 THEN 1 ELSE 0 END)                    AS suppliers_with_stock,
            CASE
                WHEN SUM(CASE WHEN ps.stock > 0 THEN 1 ELSE 0 END) = 0 THEN 'SIN_STOCK'
                WHEN SUM(CASE WHEN ps.stock > 0 THEN 1 ELSE 0 END) = 1 THEN 'RIESGO_PROVEEDOR_UNICO'
                ELSE 'CUBIERTO'
            END                                                                AS coverage_status
        FROM product_supplier ps
        JOIN producto_maestro mp ON mp.id = ps.producto_id
        GROUP BY mp.id, mp.mpn, mp.category
        ORDER BY suppliers_with_stock ASC, mp.mpn
        LIMIT :maxRows
        """, nativeQuery = true)
    List<Object[]> findCostCoverageRatio(@Param("maxRows") int maxRows);

    
}
