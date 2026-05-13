package com.proveedorio.backend.service.dashboard;

import com.proveedorio.backend.dtos.dashboard.SupplierHealthDto;
import com.proveedorio.backend.repository.ProductSupplierRepository;
import com.proveedorio.backend.repository.SyncLogRepository;
import com.proveedorio.backend.repository.SupplierRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Servicio que calcula los 6 KPIs operativos de salud de proveedor para el Dashboard:
 * SLA Score, Sync Success Rate, Avg Items/Sync, Avg Latency, API Error Rate, Stale Rate.
 */
@Service
public class SupplierHealthService {

    private final SyncLogRepository syncLogRepository;
    private final ProductSupplierRepository productSupplierRepository;
    private final SupplierRepository supplierRepository;

    public SupplierHealthService(SyncLogRepository syncLogRepository,
                                  ProductSupplierRepository productSupplierRepository,
                                  SupplierRepository supplierRepository) {
        this.syncLogRepository = syncLogRepository;
        this.productSupplierRepository = productSupplierRepository;
        this.supplierRepository = supplierRepository;
    }

    /**
     * Obtiene los KPIs de salud de todos los proveedores activos.
     * Combina datos de SyncLog (success rate, latencia, items) y ProductSupplier
     * (error rate, stale rate) y calcula el SLA Score compuesto.
     */
    @Transactional(readOnly = true)
    public List<SupplierHealthDto> getAllSupplierHealth() {

        // Construir mapas indexados por supplierId para cada metrica
        Map<Integer, double[]> successRateMap = buildSuccessRateMap();
        Map<Integer, double[]> avgItemsMap = buildAvgItemsMap();
        Map<Integer, Long> avgLatencyMap = buildAvgLatencyMap();
        Map<Integer, double[]> errorRateMap = buildErrorRateMap();
        Map<Integer, double[]> staleRateMap = buildStaleRateMap();

        // Obtener todos los supplierId conocidos (union de todos los mapas)
        Set<Integer> allSupplierIds = new HashSet<>();
        allSupplierIds.addAll(successRateMap.keySet());
        allSupplierIds.addAll(avgItemsMap.keySet());
        allSupplierIds.addAll(avgLatencyMap.keySet());
        allSupplierIds.addAll(errorRateMap.keySet());
        allSupplierIds.addAll(staleRateMap.keySet());

        // Mapa nombre de proveedor por id
        Map<Integer, String> supplierNameMap = buildSupplierNameMap(allSupplierIds,
                successRateMap, avgItemsMap, errorRateMap, staleRateMap);

        return allSupplierIds.stream()
                .map(supplierId -> buildDto(supplierId,
                        supplierNameMap.getOrDefault(supplierId, "Proveedor " + supplierId),
                        successRateMap.getOrDefault(supplierId, new double[]{0, 0, 100}),
                        avgItemsMap.getOrDefault(supplierId, new double[]{0}),
                        avgLatencyMap.getOrDefault(supplierId, 0L),
                        errorRateMap.getOrDefault(supplierId, new double[]{0}),
                        staleRateMap.getOrDefault(supplierId, new double[]{0})))
                .sorted(Comparator.comparingDouble(SupplierHealthDto::slaScore).reversed())
                .collect(Collectors.toList());
    }

    private SupplierHealthDto buildDto(Integer supplierId, String supplierName,
                                        double[] successData, double[] avgItemsData,
                                        Long avgLatencyMs, double[] errorData, double[] staleData) {
        double successRate = successData[0];    // porcentaje 0-100
        double avgItems = avgItemsData[0];
        long latencyMs = avgLatencyMs;
        double errorRate = errorData[0];
        double staleRate = staleData[0];

        // stockoutRate proxy: staleRate (datos obsoletos como indicador de calidad)
        double stockoutRate = staleRate;

        // latencyScore: cada segundo de latencia resta 1 punto, minimo 0
        double latencyScore = Math.max(0.0, 100.0 - (latencyMs / 1000.0));

        // SLA Score compuesto (0-100)
        double slaScore = (successRate * 0.4) + (latencyScore * 0.3) + ((100.0 - stockoutRate) * 0.3);
        slaScore = Math.min(100.0, Math.max(0.0, slaScore));

        String slaGrade = slaScore >= 80 ? "A" : slaScore >= 60 ? "B" : slaScore >= 40 ? "C" : "D";

        return new SupplierHealthDto(
                supplierId,
                supplierName,
                Math.round(slaScore * 100.0) / 100.0,
                Math.round(successRate * 100.0) / 100.0,
                Math.round(avgItems * 10.0) / 10.0,
                latencyMs,
                Math.round(errorRate * 100.0) / 100.0,
                Math.round(staleRate * 100.0) / 100.0,
                slaGrade
        );
    }

    // --- helpers para construir mapas ---

    private Map<Integer, double[]> buildSuccessRateMap() {
        Map<Integer, double[]> map = new HashMap<>();
        for (Object[] row : syncLogRepository.findSyncSuccessRatePerSupplier()) {
            Integer id = toInt(row[0]);
            double successRate = toDouble(row[4]); // success_rate_pct
            map.put(id, new double[]{successRate});
        }
        return map;
    }

    private Map<Integer, double[]> buildAvgItemsMap() {
        Map<Integer, double[]> map = new HashMap<>();
        for (Object[] row : syncLogRepository.findAvgItemsPerSyncPerSupplier()) {
            Integer id = toInt(row[0]);
            double avgItems = toDouble(row[2]); // avg_items
            map.put(id, new double[]{avgItems});
        }
        return map;
    }

    private Map<Integer, Long> buildAvgLatencyMap() {
        Map<Integer, Long> map = new HashMap<>();
        for (Object[] row : syncLogRepository.findAvgItemsPerSyncPerSupplier()) {
            // We need latency from a different query - use findAverageLatencyBySupplierId per supplier
        }
        // Use latency trend data for the most recent week
        for (Object[] row : syncLogRepository.findLatencyTrendPerSupplier()) {
            Integer id = toInt(row[0]);
            long latency = toLong(row[3]); // avg_latency_ms
            // Keep the latest (last entry wins since ordered by week)
            map.put(id, latency);
        }
        return map;
    }

    private Map<Integer, double[]> buildErrorRateMap() {
        Map<Integer, double[]> map = new HashMap<>();
        for (Object[] row : productSupplierRepository.findApiErrorRatePerSupplier()) {
            Integer id = toInt(row[0]);
            double errorRate = toDouble(row[4]); // error_rate_pct
            map.put(id, new double[]{errorRate});
        }
        return map;
    }

    private Map<Integer, double[]> buildStaleRateMap() {
        Map<Integer, double[]> map = new HashMap<>();
        for (Object[] row : productSupplierRepository.findStaleRatePerSupplier()) {
            Integer id = toInt(row[0]);
            double staleRate = toDouble(row[4]); // stale_rate_pct
            map.put(id, new double[]{staleRate});
        }
        return map;
    }

    private Map<Integer, String> buildSupplierNameMap(Set<Integer> ids,
            Map<Integer, double[]> successRateMap,
            Map<Integer, double[]> avgItemsMap,
            Map<Integer, double[]> errorRateMap,
            Map<Integer, double[]> staleRateMap) {
        Map<Integer, String> nameMap = new HashMap<>();
        // Pull names from error rate map (has supplier name at index 1)
        for (Object[] row : productSupplierRepository.findApiErrorRatePerSupplier()) {
            nameMap.put(toInt(row[0]), String.valueOf(row[1]));
        }
        // Fill remaining from sync success rate (has supplier name at index 1)
        for (Object[] row : syncLogRepository.findSyncSuccessRatePerSupplier()) {
            nameMap.putIfAbsent(toInt(row[0]), String.valueOf(row[1]));
        }
        return nameMap;
    }

    // --- type converters ---

    private Integer toInt(Object v) {
        if (v == null) return 0;
        if (v instanceof Number n) return n.intValue();
        try { return Integer.parseInt(v.toString()); } catch (Exception e) { return 0; }
    }

    private double toDouble(Object v) {
        if (v == null) return 0.0;
        if (v instanceof Number n) return n.doubleValue();
        try { return Double.parseDouble(v.toString()); } catch (Exception e) { return 0.0; }
    }

    private long toLong(Object v) {
        if (v == null) return 0L;
        if (v instanceof Number n) return n.longValue();
        try { return Long.parseLong(v.toString()); } catch (Exception e) { return 0L; }
    }
}
