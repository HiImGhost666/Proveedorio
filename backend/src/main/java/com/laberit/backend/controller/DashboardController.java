package com.laberit.backend.controller;

import com.laberit.backend.dtos.dashboard.DashboardSummaryDto;
import com.laberit.backend.dtos.dashboard.SupplierHealthDto;
import com.laberit.backend.service.DashboardService;
import com.laberit.backend.service.ExportService;
import com.laberit.backend.service.dashboard.SupplierHealthService;
import com.laberit.backend.util.ApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;

/**
 * Controlador REST del dashboard: resumen ejecutivo (KPIs, gráficos, proveedores caídos),
 * exportación ZIP y salud de proveedores (Supplier Health KPIs).
 * Rutas bajo /api/v1/dashboard. Mensajes en español.
 */
@RestController
@RequestMapping("/api/v1/dashboard")
public class DashboardController {

    private static final Logger logger = LoggerFactory.getLogger(DashboardController.class);
    private final DashboardService dashboardService;
    private final ExportService exportService;
    private final SupplierHealthService supplierHealthService;

    public DashboardController(DashboardService dashboardService, ExportService exportService,
                                SupplierHealthService supplierHealthService) {
        this.dashboardService = dashboardService;
        this.exportService = exportService;
        this.supplierHealthService = supplierHealthService;
    }

    /**
     * Devuelve el resumen del dashboard (totales, disponibilidad, gráficos, zombies, proveedores caídos).
     */
    @GetMapping("/summary")
    public ResponseEntity<ApiResponse<DashboardSummaryDto>> getSummary() {
        logger.info("GET /api/v1/dashboard/summary - Solicitando resumen ejecutivo");
        DashboardSummaryDto summary = dashboardService.getDashboardSummary();
        return ResponseEntity.ok(ApiResponse.success(summary, "Resumen del dashboard obtenido con éxito"));
    }

    /**
     * Devuelve los KPIs de salud operativa de todos los proveedores activos.
     * Incluye: SLA Score, Sync Success Rate, Avg Items/Sync, Avg Latency,
     * API Error Rate, Stale Rate. Solo accesible para ADMIN.
     */
    @GetMapping("/supplier-health")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<SupplierHealthDto>>> getSupplierHealth() {
        logger.info("GET /api/v1/dashboard/supplier-health - Calculando KPIs de salud de proveedores");
        List<SupplierHealthDto> data = supplierHealthService.getAllSupplierHealth();
        return ResponseEntity.ok(ApiResponse.success(data, "Supplier Health KPIs calculados correctamente"));
    }

    /**
     * Genera y descarga un ZIP con CSV del dashboard (KPIs, stock por proveedor, top productos, etc.).
     */
    @GetMapping("/export/csv")
    public ResponseEntity<byte[]> exportDashboardCsv() {
        logger.info("GET /api/v1/dashboard/export/csv - Iniciando descarga de reporte ZIP");
        try {
            DashboardSummaryDto summary = dashboardService.getDashboardSummary();
            byte[] zipData = exportService.generateDashboardZip(summary);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("application/zip"));
            headers.setContentDispositionFormData("attachment", "dashboard_data.zip");
            headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

            return ResponseEntity.ok()
                    .headers(headers)
                    .body(zipData);
        } catch (IOException e) {
            logger.error("Error generando ZIP de exportación", e);
            return ResponseEntity.internalServerError().build();
        }
    }
}
