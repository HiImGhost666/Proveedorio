package com.proveedorio.backend.service;

import com.proveedorio.backend.entity.Supplier;
import com.proveedorio.backend.entity.SyncLog;
import com.proveedorio.backend.repository.SyncLogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Service
public class ApiClientService {
    private static final Logger logger = LoggerFactory.getLogger(ApiClientService.class);
    
    private final EncryptionService encryptionService;
    private final SyncLogRepository syncLogRepository;
    private final NotificationService notificationService;
    private final HttpClient httpClient;

    public ApiClientService(EncryptionService encryptionService,
                            SyncLogRepository syncLogRepository,
                            NotificationService notificationService) {
        this.encryptionService = encryptionService;
        this.syncLogRepository = syncLogRepository;
        this.notificationService = notificationService;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(10))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();
    }

    public String callProviderApi(Supplier supplier, String sku) {
        String apiKey = decryptKey(supplier.getApiKey());
        String encodedSku = URLEncoder.encode(sku, StandardCharsets.UTF_8);
        String path = supplier.getDetailEndpoint().replace("{sku}", encodedSku);
        String finalUrl = buildUrl(supplier.getBaseUrlApi(), path);
        HttpRequest request = buildRequest(finalUrl, apiKey);
        return executeRequest(request, supplier, "Detalle Producto (" + sku + ")");
    }

    public String searchProducts(Supplier supplier, String query) {
        String apiKey = decryptKey(supplier.getApiKey());
        String encodedQuery = URLEncoder.encode(query, StandardCharsets.UTF_8);
        String path = supplier.getSearchEndpoint().replace("{query}", encodedQuery);
        String finalUrl = buildUrl(supplier.getBaseUrlApi(), path);
        HttpRequest request = buildRequest(finalUrl, apiKey);
        return executeRequest(request, supplier, "Búsqueda (" + query + ")");
    }

    public String fetchCatalog(Supplier supplier) {
        String apiKey = decryptKey(supplier.getApiKey());
        String finalUrl = buildUrl(supplier.getBaseUrlApi(), supplier.getCatalogEndpoint());
        HttpRequest request = buildRequest(finalUrl, apiKey);
        return executeRequest(request, supplier, "Catálogo Completo");
    }

    private HttpRequest buildRequest(String url, String apiKey) {
        var builder = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Accept", "application/json")
                .header("Content-Type", "application/json")
                .header("User-Agent", "InventoryApp/1.0");

        if (apiKey != null && !apiKey.equals("NO_API_KEY") && !apiKey.isEmpty()) {
            builder.header("Authorization", "Bearer " + apiKey);
        }
        return builder.GET().build();
    }

    private String buildUrl(String baseUrl, String path) {
        if (baseUrl == null || path == null) return "";
        baseUrl = baseUrl.trim();
        path = path.trim();
        if (baseUrl.endsWith("/") && path.startsWith("/")) {
            return baseUrl + path.substring(1);
        } else if (!baseUrl.endsWith("/") && !path.startsWith("/")) {
            return baseUrl + "/" + path;
        } else {
            return baseUrl + path;
        }
    }

    private String decryptKey(String encryptedApiKey) {
        if (encryptedApiKey == null || encryptedApiKey.isEmpty() || "NO_API_KEY".equals(encryptedApiKey)) {
            return null;
        }
        try {
            return encryptionService.decrypt(encryptedApiKey);
        } catch (Exception e) {
            logger.warn("No se pudo desencriptar la API Key: {}", e.getMessage());
            return null;
        }
    }

    private String executeRequest(HttpRequest request, Supplier supplier, String context) {
        long startTime = System.currentTimeMillis();
        try {
            logger.info(">>> [API REQUEST] {} - URL: {}", context, request.uri());
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            long latency = System.currentTimeMillis() - startTime;
            logger.info("<<< [API RESPONSE] {} - Status: {} - Latency: {}ms", context, response.statusCode(), latency);

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                saveSyncLog(supplier, latency, SyncLog.SyncStatus.SUCCESS, null);
                return response.body();
            } else {
                logger.error("!!! [API ERROR] {} - Body: {}", context, response.body());
                saveSyncLog(supplier, latency, SyncLog.SyncStatus.FAILED, 0);
                notificationService.createErrorNotification(
                        supplier,
                        context,
                        "Código " + response.statusCode()
                );
                throw new RuntimeException("Error del proveedor: Código " + response.statusCode());
            }
        } catch (Exception e) {
            long latency = System.currentTimeMillis() - startTime;
            logger.error("!!! [API EXCEPTION] {} - Error al conectar con {}: {}", context, request.uri(), e.getClass().getSimpleName(), e);
            saveSyncLog(supplier, latency, SyncLog.SyncStatus.FAILED, 0);
            notificationService.createErrorNotification(supplier, context, e.getMessage());
            throw new RuntimeException("Fallo en la conexión con el proveedor externo", e);
        }
    }

    private void saveSyncLog(Supplier supplier, long latencyMs, SyncLog.SyncStatus status, Integer itemsProcessed) {
        try {
            SyncLog log = new SyncLog(supplier, latencyMs, status, itemsProcessed);
            syncLogRepository.save(log);
        } catch (Exception e) {
            logger.error("!!! CRITICAL: No se pudo guardar el SyncLog para el proveedor {}. Causa: {}", supplier.getId(), e.getMessage());
        }
    }
}
