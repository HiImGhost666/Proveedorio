package com.proveedorio.backend.service;

import com.proveedorio.backend.dtos.dashboard.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class ExportService {

    private static final Logger logger = LoggerFactory.getLogger(ExportService.class);

    public byte[] generateDashboardZip(DashboardSummaryDto summary) throws IOException {
        logger.info("Iniciando generación de ZIP de exportación del dashboard...");
        long startTime = System.currentTimeMillis();
        
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            
            // 1. KPIs Generales
            addCsvToZip(zos, "kpis_generales.csv", generateKpisCsv(summary));

            // 2. Top Marcas
            addCsvToZip(zos, "top_marcas.csv", generateChartDataCsv(summary.productsByBrand(), "Marca", "Cantidad"));

            // 3. Stock por Proveedor
            addCsvToZip(zos, "stock_proveedores.csv", generateChartDataCsv(summary.stockBySupplier(), "Proveedor", "Stock Total"));

            // 4. Historial Sincronización
            addCsvToZip(zos, "historial_sync.csv", generateSyncHistoryCsv(summary.syncHistory()));

            // 5. Top Productos Caros
            addCsvToZip(zos, "top_productos_caros.csv", generateTopProductsCsv(summary.topExpensiveProducts()));

            // 6. Top Movers (Subidas)
            addCsvToZip(zos, "alertas_subidas_precio.csv", generateTopMoversCsv(summary.topPriceIncreases()));

            // 7. Top Movers (Bajadas)
            addCsvToZip(zos, "oportunidades_bajadas_precio.csv", generateTopMoversCsv(summary.topPriceDecreases()));
            
            // 8. Productos Zombies
            addCsvToZip(zos, "productos_zombies.csv", generateZombiesCsv(summary.staleProducts()));
        }
        
        byte[] result = baos.toByteArray();
        long endTime = System.currentTimeMillis();
        logger.info("ZIP generado exitosamente. Tamaño: {} bytes. Tiempo: {} ms", result.length, (endTime - startTime));
        return result;
    }

    private void addCsvToZip(ZipOutputStream zos, String filename, String content) throws IOException {
        ZipEntry entry = new ZipEntry(filename);
        zos.putNextEntry(entry);
        zos.write(content.getBytes(StandardCharsets.UTF_8));
        zos.closeEntry();
    }

    private String generateKpisCsv(DashboardSummaryDto summary) {
        StringBuilder sb = new StringBuilder();
        sb.append("Métrica,Valor\n");
        sb.append("Total Productos,").append(summary.totalProducts()).append("\n");
        sb.append("Total Proveedores,").append(summary.totalSuppliers()).append("\n");
        sb.append("Latencia Promedio (ms),").append(summary.avgLatencyOverall()).append("\n");
        sb.append("Productos Disponibles,").append(summary.productsAvailable()).append("\n");
        sb.append("Productos Bajo Stock,").append(summary.productsLowStock()).append("\n");
        sb.append("Productos Sin Stock,").append(summary.productsOutOfStock()).append("\n");
        return sb.toString();
    }

    private String generateChartDataCsv(List<ChartDataDto> data, String labelHeader, String valueHeader) {
        StringBuilder sb = new StringBuilder();
        sb.append(labelHeader).append(",").append(valueHeader).append("\n");
        for (ChartDataDto item : data) {
            sb.append(escapeCsv(item.label())).append(",").append(item.value()).append("\n");
        }
        return sb.toString();
    }

    private String generateSyncHistoryCsv(List<SyncHistoryDto> data) {
        StringBuilder sb = new StringBuilder();
        sb.append("Fecha,Exitos,Errores\n");
        for (SyncHistoryDto item : data) {
            sb.append(item.date()).append(",").append(item.successCount()).append(",").append(item.errorCount()).append("\n");
        }
        return sb.toString();
    }

    private String generateTopProductsCsv(List<TopProductDto> data) {
        StringBuilder sb = new StringBuilder();
        sb.append("ProductoId,Producto,Proveedor,Precio\n");
        for (TopProductDto item : data) {
            sb.append(item.productId()).append(",")
              .append(escapeCsv(item.name())).append(",")
              .append(escapeCsv(item.supplierName())).append(",")
              .append(item.price()).append("\n");
        }
        return sb.toString();
    }

    private String generateTopMoversCsv(List<TopMoverDto> data) {
        StringBuilder sb = new StringBuilder();
        sb.append("ProductoId,Producto,Proveedor,Precio Anterior,Precio Actual,Variación %\n");
        for (TopMoverDto item : data) {
            sb.append(item.productId()).append(",")
              .append(escapeCsv(item.productName())).append(",")
              .append(escapeCsv(item.supplierName())).append(",")
              .append(item.previousPrice()).append(",")
              .append(item.currentPrice()).append(",")
              .append(item.percentChange()).append("\n");
        }
        return sb.toString();
    }
    
    private String generateZombiesCsv(List<StaleProductDto> data) {
        StringBuilder sb = new StringBuilder();
        sb.append("ID,MPN,Producto,Última Actualización,Días Inactivo\n");
        for (StaleProductDto item : data) {
            sb.append(item.id()).append(",")
              .append(escapeCsv(item.mpn())).append(",")
              .append(escapeCsv(item.name())).append(",")
              .append(item.lastUpdatedAt()).append(",")
              .append(item.daysWithoutUpdate()).append("\n");
        }
        return sb.toString();
    }

    private String escapeCsv(String value) {
        if (value == null) return "";
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}

