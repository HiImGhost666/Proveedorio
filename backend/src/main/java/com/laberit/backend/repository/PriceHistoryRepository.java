package com.laberit.backend.repository;

import com.laberit.backend.dtos.dashboard.TopMoverDto;
import com.laberit.backend.entity.MasterProduct;
import com.laberit.backend.entity.PriceHistory;
import com.laberit.backend.entity.Supplier;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repositorio de acceso a datos para la entidad {@link PriceHistory}.
 * Tabla en base de datos: "historial_precios"; columnas en español.
 */
@Repository
public interface PriceHistoryRepository extends JpaRepository<PriceHistory, Integer> {

    List<PriceHistory> findByMasterProductId(Integer masterProductId);

    Optional<PriceHistory> findFirstByMasterProductAndSupplierOrderByRegisteredAtDesc(MasterProduct masterProduct, Supplier supplier);

    List<PriceHistory> findByMasterProductAndSupplierOrderByRegisteredAtDesc(MasterProduct masterProduct, Supplier supplier);

    /**
     * Devuelve movimientos recientes de precios para construir alertas de "Top Movers".
     * <p>
     * El cálculo real de variaciones porcentuales se realiza en memoria; aquí solo
     * se proyectan los campos necesarios y se ordena por fecha de registro.
     */
    @Query("SELECT new com.laberit.backend.dtos.dashboard.TopMoverDto(" +
           "CONCAT(h.masterProduct.brand, ' ', h.masterProduct.model), " +
           "h.supplier.name, " +
           "CAST(0 AS bigdecimal), " +
           "h.costPrice, " +
           "CAST(0 AS double)) " +
           "FROM PriceHistory h " +
           "ORDER BY h.registeredAt DESC")
    List<TopMoverDto> findRecentMovements(Pageable pageable);

    /**
     * Obtiene IDs de productos que han tenido actividad reciente en historial de precios,
     * ordenados por la fecha del último cambio. Se usa para limitar el análisis a los
     * productos más activos.
     */
    @Query("SELECT DISTINCT h.masterProduct.id FROM PriceHistory h ORDER BY h.registeredAt DESC")
    List<Integer> findProductIdsWithRecentActivity(Pageable pageable);

    /**
     * Calcula la variacion mensual del indice de precios por proveedor usando window function LAG.
     *
     * Logica:
     *   1. Agrupa historial_precios por proveedor, producto y mes (formato YYYY-MM).
     *   2. Calcula el precio promedio mensual normalizado a la moneda base (EUR) usando
     *      la tabla currency_rates. Si no hay tasa disponible se asume factor 1.0.
     *   3. Aplica LAG() para obtener el precio promedio del mes anterior.
     *   4. Calcula la variacion porcentual: ((actual - anterior) / anterior) * 100.
     *   5. Excluye filas donde la variacion supera el umbral de outlier (param :outlierThreshold).
     *
     * Requiere MariaDB >= 10.2 (window functions). Compatible con MariaDB 10.11.
     *
     * @param months           numero de meses hacia atras a analizar (ej: 6)
     * @param outlierThreshold umbral de variacion absoluta para filtrar outliers (ej: 50.0)
     * @return lista de Object[] con columnas: supplier_id, supplier_name, product_id, mpn,
     *         month, avg_price, prev_avg_price, variation_pct
     */
    /**
     * Price Stability Score por SKU/proveedor: desviacion estandar y promedio del precio
     * en los ultimos :days dias. Clasifica por coeficiente de variacion (CV = stddev/avg).
     * Columnas: product_id, mpn, supplier_id, supplier_name, avg_price, stddev_price,
     *           cv_pct, stability_label, price_points
     */
    @Query(value = """
        SELECT
            mp.id                                                AS product_id,
            mp.mpn                                              AS mpn,
            s.id                                                AS supplier_id,
            s.nombre                                            AS supplier_name,
            ROUND(AVG(hp.precio_costo), 4)                      AS avg_price,
            ROUND(STDDEV(hp.precio_costo), 4)                   AS stddev_price,
            ROUND(STDDEV(hp.precio_costo) / NULLIF(AVG(hp.precio_costo), 0) * 100, 2) AS cv_pct,
            CASE
                WHEN STDDEV(hp.precio_costo) / NULLIF(AVG(hp.precio_costo), 0) * 100 < 5
                THEN 'ESTABLE'
                WHEN STDDEV(hp.precio_costo) / NULLIF(AVG(hp.precio_costo), 0) * 100 <= 15
                THEN 'MODERADO'
                ELSE 'VOLATIL'
            END                                                  AS stability_label,
            COUNT(*)                                             AS price_points
        FROM historial_precios hp
        JOIN proveedores s      ON s.id  = hp.proveedor_id
        JOIN producto_maestro mp ON mp.id = hp.producto_id
        WHERE hp.fecha_registro >= DATE_SUB(NOW(), INTERVAL :days DAY)
        GROUP BY mp.id, mp.mpn, s.id, s.nombre
        HAVING COUNT(*) >= 2
        ORDER BY cv_pct DESC
        LIMIT :maxRows
        """, nativeQuery = true)
    List<Object[]> findPriceStabilityScores(@Param("days") int days, @Param("maxRows") int maxRows);

    @Query(value = """
        WITH monthly_avg AS (
            SELECT
                s.id                                   AS supplier_id,
                s.nombre                               AS supplier_name,
                mp.id                                  AS product_id,
                mp.mpn                                 AS mpn,
                DATE_FORMAT(hp.fecha_registro, '%Y-%m') AS month,
                AVG(hp.precio_costo
                    * COALESCE(
                        (SELECT cr.rate FROM currency_rates cr
                         WHERE cr.from_currency = COALESCE(ps.currency, 'EUR')
                           AND cr.to_currency   = 'EUR'
                         LIMIT 1),
                        1.0)
                )                                      AS avg_price
            FROM historial_precios hp
            JOIN proveedores     s  ON hp.proveedor_id = s.id
            JOIN producto_maestro mp ON hp.producto_id  = mp.id
            LEFT JOIN product_supplier ps
                   ON ps.producto_id  = mp.id
                  AND ps.proveedor_id = s.id
            WHERE hp.fecha_registro >= DATE_SUB(NOW(), INTERVAL :months MONTH)
            GROUP BY s.id, s.nombre, mp.id, mp.mpn,
                     DATE_FORMAT(hp.fecha_registro, '%Y-%m')
        ),
        lagged AS (
            SELECT
                supplier_id,
                supplier_name,
                product_id,
                mpn,
                month,
                avg_price,
                LAG(avg_price) OVER (
                    PARTITION BY supplier_id, product_id
                    ORDER BY month
                ) AS prev_avg_price,
                CASE
                    WHEN LAG(avg_price) OVER (
                             PARTITION BY supplier_id, product_id
                             ORDER BY month
                         ) IS NOT NULL
                         AND LAG(avg_price) OVER (
                             PARTITION BY supplier_id, product_id
                             ORDER BY month
                         ) <> 0
                    THEN ROUND(
                        ((avg_price - LAG(avg_price) OVER (
                            PARTITION BY supplier_id, product_id ORDER BY month))
                         / LAG(avg_price) OVER (
                            PARTITION BY supplier_id, product_id ORDER BY month))
                        * 100, 4)
                    ELSE NULL
                END AS variation_pct
            FROM monthly_avg
        )
        SELECT supplier_id, supplier_name, product_id, mpn,
               month, avg_price, prev_avg_price, variation_pct
        FROM lagged
        WHERE variation_pct IS NULL
           OR ABS(variation_pct) <= :outlierThreshold
        ORDER BY supplier_name, product_id, month
        """,
        nativeQuery = true)
    List<Object[]> findPriceIndexVariation(
        @Param("months") int months,
        @Param("outlierThreshold") double outlierThreshold
    );

    @Query(value = """
        WITH monthly_avg AS (
            SELECT
                s.id                                   AS supplier_id,
                s.nombre                               AS supplier_name,
                mp.id                                  AS product_id,
                mp.mpn                                 AS mpn,
                DATE_FORMAT(hp.fecha_registro, '%Y-%m') AS month,
                AVG(hp.precio_costo)                   AS avg_price
            FROM historial_precios hp
            JOIN proveedores     s  ON hp.proveedor_id = s.id
            JOIN producto_maestro mp ON hp.producto_id  = mp.id
            WHERE hp.fecha_registro >= DATE_SUB(NOW(), INTERVAL :months MONTH)
            GROUP BY s.id, s.nombre, mp.id, mp.mpn,
                     DATE_FORMAT(hp.fecha_registro, '%Y-%m')
        ),
        lagged AS (
            SELECT
                supplier_id,
                supplier_name,
                product_id,
                mpn,
                month,
                avg_price,
                LAG(avg_price) OVER (
                    PARTITION BY supplier_id, product_id
                    ORDER BY month
                ) AS prev_avg_price,
                CASE
                    WHEN LAG(avg_price) OVER (
                             PARTITION BY supplier_id, product_id
                             ORDER BY month
                         ) IS NOT NULL
                         AND LAG(avg_price) OVER (
                             PARTITION BY supplier_id, product_id
                             ORDER BY month
                         ) <> 0
                    THEN ROUND(
                        ((avg_price - LAG(avg_price) OVER (
                            PARTITION BY supplier_id, product_id ORDER BY month))
                         / LAG(avg_price) OVER (
                            PARTITION BY supplier_id, product_id ORDER BY month))
                        * 100, 4)
                    ELSE NULL
                END AS variation_pct
            FROM monthly_avg
        )
        SELECT supplier_id, supplier_name, product_id, mpn,
               month, avg_price, prev_avg_price, variation_pct
        FROM lagged
        WHERE variation_pct IS NULL
           OR ABS(variation_pct) <= :outlierThreshold
        ORDER BY supplier_name, product_id, month
        """,
        nativeQuery = true)
    List<Object[]> findPriceIndexVariationWithoutCurrencyRates(
        @Param("months") int months,
        @Param("outlierThreshold") double outlierThreshold
    );
}
