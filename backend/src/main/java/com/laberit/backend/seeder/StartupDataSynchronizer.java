package com.laberit.backend.seeder;

import com.laberit.backend.repository.MasterProductRepository;
import com.laberit.backend.service.CurrencyRateService;
import com.laberit.backend.service.IStockService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Componente que dispara sincronizaciones automáticas de catálogos.
 * <ul>
 *     <li>Al arranque de la aplicación (si la base está vacía, hace una carga inicial).</li>
 *     <li>De forma periódica cada 6 horas mediante una tarea programada.</li>
 * </ul>
 * La coordinación entre réplicas se realiza a través de {@link com.laberit.backend.service.SyncStateService}.
 */
@Component
@Profile("!test")
@Order(3)
public class StartupDataSynchronizer implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(StartupDataSynchronizer.class);

    private final MasterProductRepository masterProductRepository;
    private final IStockService stockService;
    private final CurrencyRateService currencyRateService;

    public StartupDataSynchronizer(MasterProductRepository masterProductRepository,
                                    IStockService stockService,
                                    CurrencyRateService currencyRateService) {
        this.masterProductRepository = masterProductRepository;
        this.stockService = stockService;
        this.currencyRateService = currencyRateService;
    }

    /**
     * Se ejecuta una sola vez al inicio de la aplicación (ApplicationRunner).
     * Primero refresca las tasas de cambio desde la API del BCE (Frankfurter),
     * luego lanza la sincronización completa de catálogos. El refresco de tasas
     * ya fue iniciado por DataSeeder (@Order 2), este segundo refresco garantiza
     * que usamos los datos más recientes justo antes de procesar precios.
     */
    @Override
    public void run(ApplicationArguments args) throws Exception {
        logger.info("Verificando estado de la base de datos al arrancar...");
        if (masterProductRepository.count() == 0) {
            logger.info("La base de datos de productos esta vacia. Disparando sincronizacion inicial...");
        } else {
            logger.info("La base de datos ya contiene datos. Disparando sincronizacion de rutina al arranque...");
        }
        currencyRateService.refreshRates();
        stockService.syncAllProducts();
    }

    /**
     * Sincronizacion periodica cada 6 horas usando fixedRate para garantizar exactamente
     * 4 ejecuciones por dia desde el arranque, independientemente de cuanto tarde el sync.
     * initialDelay=6h evita solapamiento con la sincronizacion de arranque (run()).
     * GlobalSyncState previene ejecucion concurrente entre replicas.
     * Se refresca la tasa de cambio antes de cada ciclo para mantener los KPIs de precio actualizados.
     */
    @Scheduled(fixedRateString = "PT6H", initialDelayString = "PT6H")
    public void scheduledSync() {
        logger.info("[Scheduled] Actualizando tasas de cambio y disparando sincronizacion periodica (cada 6h)...");
        currencyRateService.refreshRates();
        stockService.syncAllProducts();
    }
}
