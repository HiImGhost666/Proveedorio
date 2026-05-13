package com.proveedorio.backend.repository;

import com.proveedorio.backend.entity.StockHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Repositorio para {@link StockHistory}.
 * Contiene la query nativa de deteccion de volatilidad de stock en ventanas de 24 horas.
 */
public interface StockHistoryRepository extends JpaRepository<StockHistory, Long> {

    /**
     * Detecta SKUs con alta volatilidad de stock en los ultimos 30 dias (ventanas de 24h).
     * Clasifica cada grupo como:
     * - INCONSISTENCIA_API: stock bajo a 0 y recupero >= 50% del maximo en <24h (3+ cambios)
     * - VENTA_PROBABLE: stock bajo significativamente y no recupero
     * - FLUCTUACION_NORMAL: otros casos con multiples cambios
     *
     * Solo devuelve SKUs con 2+ cambios en alguna ventana de 24h de los ultimos 30 dias.
     */
    @Query(value = """
        SELECT
            sh.proveedor_id              AS supplier_id,
            s.nombre                     AS supplier_name,
            sh.producto_id               AS product_id,
            mp.mpn                       AS mpn,
            COUNT(*)                     AS changes_in_24h,
            MAX(sh.stock_value)          AS max_stock,
            MIN(sh.stock_value)          AS min_stock,
            CASE
                WHEN MIN(sh.stock_value) = 0
                     AND MAX(sh.stock_value) >= 50
                     AND COUNT(*) >= 3
                THEN 'INCONSISTENCIA_API'
                WHEN MIN(sh.stock_value) < MAX(sh.stock_value) * 0.3
                     AND MIN(sh.stock_value) < MAX(sh.stock_value)
                THEN 'VENTA_PROBABLE'
                ELSE 'FLUCTUACION_NORMAL'
            END                          AS volatility_type
        FROM stock_history sh
        JOIN proveedores s  ON s.id  = sh.proveedor_id
        JOIN producto_maestro mp ON mp.id = sh.producto_id
        WHERE sh.recorded_at >= DATE_SUB(NOW(), INTERVAL :days DAY)
        GROUP BY sh.proveedor_id, s.nombre, sh.producto_id, mp.mpn,
                 FLOOR(TIMESTAMPDIFF(HOUR, DATE_SUB(NOW(), INTERVAL :days DAY), sh.recorded_at) / 24)
        HAVING COUNT(*) >= 2
        ORDER BY changes_in_24h DESC
        LIMIT :maxRows
        """, nativeQuery = true)
    List<Object[]> findStockVolatility(@Param("days") int days, @Param("maxRows") int maxRows);
}
