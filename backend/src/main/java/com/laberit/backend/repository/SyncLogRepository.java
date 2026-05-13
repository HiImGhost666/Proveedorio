package com.laberit.backend.repository;

import com.laberit.backend.entity.SyncLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SyncLogRepository extends JpaRepository<SyncLog, Long> {

    /**
     * Calcula la latencia media (en milisegundos) de sincronización para un proveedor concreto.
     */
    @Query("SELECT AVG(sl.latencyMs) FROM SyncLog sl WHERE sl.supplier.id = :supplierId")
    Double findAverageLatencyBySupplierId(@Param("supplierId") Integer supplierId);

    /**
     * Calcula la latencia media global considerando solo sincronizaciones exitosas.
     */
    @Query("SELECT AVG(sl.latencyMs) FROM SyncLog sl WHERE sl.status = 'SUCCESS'")
    Double findAverageLatencyGlobal();

    /**
     * Recupera los logs crudos desde una fecha de inicio para procesarlos en memoria
     * (agrupaciones, contadores diarios, etc.) evitando depender de funciones SQL específicas
     * del dialecto.
     */
    @Query("SELECT s FROM SyncLog s WHERE s.timestamp >= :startDate ORDER BY s.timestamp ASC")
    List<SyncLog> findLogsSince(@Param("startDate") LocalDateTime startDate);

    /**
     * Obtiene el último log registrado para cada proveedor.
     */
    @Query("SELECT s FROM SyncLog s WHERE s.timestamp IN " +
           "(SELECT MAX(s2.timestamp) FROM SyncLog s2 GROUP BY s2.supplier.id)")
    List<SyncLog> findLatestLogsPerProvider();

    /**
     * Sync Success Rate por proveedor en los ultimos 30 dias.
     * Columnas: supplier_id, supplier_name, total_syncs, success_count, success_rate_pct
     */
    @Query(value = """
        SELECT
            s.id                                              AS supplier_id,
            s.nombre                                          AS supplier_name,
            COUNT(*)                                          AS total_syncs,
            SUM(CASE WHEN sl.status = 'SUCCESS' THEN 1 ELSE 0 END) AS success_count,
            ROUND(SUM(CASE WHEN sl.status = 'SUCCESS' THEN 1 ELSE 0 END)
                  / COUNT(*) * 100, 2)                        AS success_rate_pct
        FROM sync_logs sl
        JOIN proveedores s ON s.id = sl.proveedor_id
        WHERE sl.timestamp >= DATE_SUB(NOW(), INTERVAL 30 DAY)
        GROUP BY s.id, s.nombre
        ORDER BY success_rate_pct ASC
        """, nativeQuery = true)
    List<Object[]> findSyncSuccessRatePerSupplier();

    /**
     * Avg items procesados por sync por proveedor.
     * Columnas: supplier_id, supplier_name, avg_items, total_syncs
     */
    @Query(value = """
        SELECT
            s.id              AS supplier_id,
            s.nombre          AS supplier_name,
            ROUND(AVG(COALESCE(sl.items_processed, 0)), 1) AS avg_items,
            COUNT(*)          AS total_syncs
        FROM sync_logs sl
        JOIN proveedores s ON s.id = sl.proveedor_id
        GROUP BY s.id, s.nombre
        ORDER BY avg_items DESC
        """, nativeQuery = true)
    List<Object[]> findAvgItemsPerSyncPerSupplier();

    /**
     * Tendencia de latencia semanal de los ultimos 4 semanas por proveedor.
     * Columnas: supplier_id, supplier_name, year_week, avg_latency_ms
     */
    @Query(value = """
        SELECT
            s.id                                           AS supplier_id,
            s.nombre                                       AS supplier_name,
            DATE_FORMAT(sl.timestamp, '%Y-W%u')            AS year_week,
            ROUND(AVG(sl.latency_ms), 0)                   AS avg_latency_ms
        FROM sync_logs sl
        JOIN proveedores s ON s.id = sl.proveedor_id
        WHERE sl.timestamp >= DATE_SUB(NOW(), INTERVAL 28 DAY)
        GROUP BY s.id, s.nombre, DATE_FORMAT(sl.timestamp, '%Y-W%u')
        ORDER BY s.id, year_week
        """, nativeQuery = true)
    List<Object[]> findLatencyTrendPerSupplier();
}
