package com.proveedorio.backend.service;

import com.proveedorio.backend.dtos.dashboard.*;
import com.proveedorio.backend.dtos.supplier.ProviderStatusDto;
import com.proveedorio.backend.entity.PriceHistory;
import com.proveedorio.backend.entity.MasterProduct;
import com.proveedorio.backend.entity.SyncLog;
import com.proveedorio.backend.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class DashboardService {

    private static final Logger logger = LoggerFactory.getLogger(DashboardService.class);
    private final MasterProductRepository masterProductRepository;
    private final SupplierRepository supplierRepository;
    private final SyncLogRepository syncLogRepository;
    private final ProductSupplierRepository productSupplierRepository;
    private final PriceHistoryRepository priceHistoryRepository;
    private static final int MIN_STOCK_THRESHOLD = 5;

    public DashboardService(
            MasterProductRepository masterProductRepository,
            SupplierRepository supplierRepository,
            SyncLogRepository syncLogRepository,
            ProductSupplierRepository productSupplierRepository,
            PriceHistoryRepository priceHistoryRepository) {
        this.masterProductRepository = masterProductRepository;
        this.supplierRepository = supplierRepository;
        this.syncLogRepository = syncLogRepository;
        this.productSupplierRepository = productSupplierRepository;
        this.priceHistoryRepository = priceHistoryRepository;
    }

    @Transactional(readOnly = true)
    public DashboardSummaryDto getDashboardSummary() {
        logger.info("Generando resumen del dashboard...");
        long startTime = System.currentTimeMillis();

        try {
            // 1. Métricas KPI
            long totalProductos = masterProductRepository.count();
            long totalProveedores = supplierRepository.count();
            
            // Eliminado cálculo de valorTotalInventario

            Double avgLatencyMs = syncLogRepository.findAverageLatencyGlobal();
            double latenciaPromedioGeneral = (avgLatencyMs != null) ? avgLatencyMs : 0.0;

            long productosDisponibles = masterProductRepository.countProductosDisponibles(MIN_STOCK_THRESHOLD);
            long productosBajoStock = masterProductRepository.countProductosBajoStock(MIN_STOCK_THRESHOLD);
            long productosSinStock = totalProductos - productosDisponibles - productosBajoStock;
            if (productosSinStock < 0) productosSinStock = 0;

            logger.debug("KPIs calculados: TotalProductos={}", totalProductos);

            // 2. Datos para Gráficos
            List<ChartDataDto> productosPorMarca = masterProductRepository.findTopBrands(PageRequest.of(0, 5));
            List<ChartDataDto> stockPorProveedor = productSupplierRepository.findStockByProvider(PageRequest.of(0, 5));
            
            // Historial Sincronización (Cálculo en Memoria)
            LocalDateTime sevenDaysAgo = LocalDateTime.now().minusDays(7);
            List<SyncLog> logsRecientes = syncLogRepository.findLogsSince(sevenDaysAgo);
            
            Map<String, SyncHistoryDto> historialMap = new LinkedHashMap<>();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM");

            for (int i = 6; i >= 0; i--) {
                String fechaKey = LocalDateTime.now().minusDays(i).format(formatter);
                historialMap.put(fechaKey, new SyncHistoryDto(fechaKey, 0, 0));
            }

            for (SyncLog log : logsRecientes) {
                String fechaKey = log.getTimestamp().format(formatter);
                if (historialMap.containsKey(fechaKey)) {
                    SyncHistoryDto current = historialMap.get(fechaKey);
                    long exitos = current.successCount() + (log.getStatus() == SyncLog.SyncStatus.SUCCESS ? 1 : 0);
                    long errores = current.errorCount() + (log.getStatus() == SyncLog.SyncStatus.FAILED ? 1 : 0);
                    historialMap.put(fechaKey, new SyncHistoryDto(fechaKey, exitos, errores));
                }
            }
            List<SyncHistoryDto> historialSincronizacion = new ArrayList<>(historialMap.values());

            List<TopProductDto> topProductosMasCaros = productSupplierRepository.findTopExpensiveProducts(PageRequest.of(0, 5));

            // 3. Top Movers (Cálculo en Memoria)
            logger.debug("Calculando Top Movers (variaciones de precio)...");
            Page<PriceHistory> ultimosMovimientos = priceHistoryRepository.findAll(
                PageRequest.of(0, 100, Sort.by(Sort.Direction.DESC, "registeredAt"))
            );

            List<TopMoverDto> allMovers = new ArrayList<>();
            Map<String, List<PriceHistory>> historiaPorProducto = ultimosMovimientos.getContent().stream()
                .collect(Collectors.groupingBy(h -> h.getMasterProduct().getId() + "-" + h.getSupplier().getId()));

            for (List<PriceHistory> lista : historiaPorProducto.values()) {
                if (lista.size() >= 2) {
                    PriceHistory actual = lista.get(0);
                    PriceHistory anterior = lista.get(1);

                    BigDecimal precioActual = actual.getCostPrice();
                    BigDecimal precioAnterior = anterior.getCostPrice();
                    
                    if (precioActual != null && precioAnterior != null && precioAnterior.compareTo(BigDecimal.ZERO) > 0) {
                        BigDecimal diff = precioActual.subtract(precioAnterior);
                        double variacion = diff.divide(precioAnterior, 4, RoundingMode.HALF_UP).doubleValue() * 100;
                        
                        if (Math.abs(variacion) > 0.01) {
                            allMovers.add(new TopMoverDto(
                                actual.getMasterProduct().getId(),
                                actual.getMasterProduct().getBrand() + " " + actual.getMasterProduct().getModel(),
                                actual.getSupplier().getName(),
                                precioAnterior,
                                precioActual,
                                variacion
                            ));
                        }
                    }
                }
            }

            List<TopMoverDto> mayoresSubidas = allMovers.stream()
                .filter(m -> m.percentChange() > 0)
                .sorted(Comparator.comparingDouble(TopMoverDto::percentChange).reversed())
                .limit(5)
                .collect(Collectors.toList());

            List<TopMoverDto> mayoresBajadas = allMovers.stream()
                .filter(m -> m.percentChange() < 0)
                .sorted(Comparator.comparingDouble(TopMoverDto::percentChange))
                .limit(5)
                .collect(Collectors.toList());

            // 4. Productos Zombies
            logger.debug("Buscando productos zombies...");
            LocalDateTime treintaDiasAtras = LocalDateTime.now().minusDays(30);
            List<MasterProduct> zombies = masterProductRepository.findStaleProducts(treintaDiasAtras);

            List<StaleProductDto> productosZombies = zombies.stream()
                .limit(10)
                .map(p -> new StaleProductDto(
                    p.getId(),
                    p.getMpn(),
                    p.getBrand() + " " + p.getModel(),
                    p.getFechaActualizacion(),
                    java.time.temporal.ChronoUnit.DAYS.between(p.getFechaActualizacion(), LocalDateTime.now())
                ))
                .collect(Collectors.toList());

            // 5. Estado del Sistema (Proveedores Caídos)
            logger.debug("Verificando estado de proveedores...");
            List<SyncLog> ultimosLogs = syncLogRepository.findLatestLogsPerProvider();
            List<ProviderStatusDto> proveedoresCaidos = ultimosLogs.stream()
                .filter(log -> log.getStatus() == SyncLog.SyncStatus.FAILED)
                .map(log -> new ProviderStatusDto(
                    log.getSupplier().getId(),
                    log.getSupplier().getName(),
                    "Fallo en la última sincronización (" + log.getTimestamp().format(DateTimeFormatter.ofPattern("dd/MM HH:mm")) + ")"
                ))
                .collect(Collectors.toList());

            long endTime = System.currentTimeMillis();
            logger.info("Dashboard generado en {} ms. Zombies: {}, Caídos: {}", (endTime - startTime), productosZombies.size(), proveedoresCaidos.size());

            return new DashboardSummaryDto(
                totalProductos,
                totalProveedores,
                productosDisponibles,
                productosBajoStock,
                productosSinStock,
                // valorTotalInventario eliminado
                latenciaPromedioGeneral,
                productosPorMarca,
                stockPorProveedor,
                historialSincronizacion,
                topProductosMasCaros,
                mayoresSubidas,
                mayoresBajadas,
                productosZombies,
                proveedoresCaidos
            );
        } catch (Exception e) {
            logger.error("Error crítico generando el dashboard", e);
            throw e;
        }
    }
}

