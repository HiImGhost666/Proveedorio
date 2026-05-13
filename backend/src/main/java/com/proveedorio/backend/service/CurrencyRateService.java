package com.proveedorio.backend.service;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.proveedorio.backend.entity.CurrencyRate;
import com.proveedorio.backend.repository.CurrencyRateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;

/**
 * Servicio de actualización de tasas de cambio.
 *
 * <p>Consulta la API pública de <b>Frankfurter</b> (https://api.frankfurter.app),
 * que publica diariamente los tipos de cambio oficiales del Banco Central Europeo
 * sin necesidad de API key. Si la API no está disponible, aplica tasas de respaldo
 * hardcodeadas para garantizar que el sistema siempre tenga datos operativos.</p>
 *
 * <p>Flujo de datos:</p>
 * <pre>
 *   GET /latest?from=EUR&to=USD,GBP,CHF,JPY
 *   → { "base": "EUR", "date": "2025-01-15", "rates": { "USD": 1.034, ... } }
 *   → almacena EUR→USD y USD→EUR (invertida) en currency_rates
 * </pre>
 *
 * <p>Llamado en {@link com.proveedorio.backend.seeder.StartupDataSynchronizer}
 * antes de cada sincronización de catálogos: primero tasas, luego productos.</p>
 */
@Service
public class CurrencyRateService {

    private static final Logger log = LoggerFactory.getLogger(CurrencyRateService.class);

    private static final String FRANKFURTER_BASE = "https://api.frankfurter.app";
    private static final String RATES_PATH = "/latest?from=EUR&to=USD,GBP,CHF,JPY";

    /**
     * Tasas de respaldo (BCE aproximadas enero 2025) usadas si la API no responde.
     * Estas son tasas EUR→X (cuántas unidades de X vale 1 EUR).
     */
    private static final Map<String, BigDecimal> FALLBACK_EUR_TO_X = Map.of(
        "USD", new BigDecimal("1.0870"),
        "GBP", new BigDecimal("0.8540"),
        "CHF", new BigDecimal("0.9420"),
        "JPY", new BigDecimal("161.50")
    );

    private final CurrencyRateRepository currencyRateRepository;
    private final RestClient restClient;

    public CurrencyRateService(CurrencyRateRepository currencyRateRepository,
                                RestClient.Builder restClientBuilder) {
        this.currencyRateRepository = currencyRateRepository;

        // Timeout corto para no bloquear el arranque si la API está caída
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(8_000);

        this.restClient = restClientBuilder
                .baseUrl(FRANKFURTER_BASE)
                .requestFactory(factory)
                .defaultHeader("Accept", "application/json")
                .defaultHeader("User-Agent", "Web-App-Inventario/1.0")
                .build();
    }

    /**
     * Actualiza todos los pares de moneda en la tabla {@code currency_rates}.
     *
     * <ul>
     *   <li>Siempre garantiza EUR→EUR = 1.0</li>
     *   <li>Para cada moneda soportada almacena EUR→X y X→EUR (invertida)</li>
     *   <li>Solo actualiza la BD si la tasa realmente cambió (evita escrituras redundantes)</li>
     *   <li>Si la API falla, aplica tasas de respaldo hardcodeadas sin lanzar excepción</li>
     * </ul>
     *
     * @return {@code true} si las tasas provienen de la API en vivo,
     *         {@code false} si se usaron las tasas de respaldo
     */
    @Transactional
    public boolean refreshRates() {
        log.info("[CurrencyRates] Actualizando tasas de cambio desde Frankfurter (BCE)...");

        // EUR → EUR = 1.0 siempre (identidad; simplifica las queries de normalización)
        upsertRate("EUR", "EUR", BigDecimal.ONE);

        try {
            FrankfurterResponse response = restClient.get()
                    .uri(RATES_PATH)
                    .retrieve()
                    .body(FrankfurterResponse.class);

            if (response == null || response.rates() == null || response.rates().isEmpty()) {
                log.warn("[CurrencyRates] Respuesta de Frankfurter vacía o inválida. Usando tasas de respaldo.");
                applyFallbackRates();
                return false;
            }

            for (Map.Entry<String, Double> entry : response.rates().entrySet()) {
                String currency = entry.getKey();
                BigDecimal eurToX = BigDecimal.valueOf(entry.getValue())
                        .setScale(6, RoundingMode.HALF_UP);
                BigDecimal xToEur = BigDecimal.ONE
                        .divide(eurToX, 6, RoundingMode.HALF_UP);

                upsertRate("EUR", currency, eurToX);  // EUR → USD/GBP/CHF/JPY
                upsertRate(currency, "EUR", xToEur);  // USD/GBP/CHF/JPY → EUR
            }

            log.info("[CurrencyRates] Tasas actualizadas desde BCE (fecha publicación: {}). Pares: EUR ↔ {}",
                    response.date(), response.rates().keySet());
            return true;

        } catch (Exception e) {
            log.warn("[CurrencyRates] No se pudo contactar Frankfurter API: {}. Usando tasas de respaldo.",
                    e.getMessage());
            applyFallbackRates();
            return false;
        }
    }

    /**
     * Aplica las tasas de respaldo hardcodeadas.
     * Se usa cuando la API de Frankfurter no está disponible (red, timeout, etc.).
     */
    private void applyFallbackRates() {
        for (Map.Entry<String, BigDecimal> entry : FALLBACK_EUR_TO_X.entrySet()) {
            String currency = entry.getKey();
            BigDecimal eurToX = entry.getValue();
            BigDecimal xToEur = BigDecimal.ONE.divide(eurToX, 6, RoundingMode.HALF_UP);
            upsertRate("EUR", currency, eurToX);
            upsertRate(currency, "EUR", xToEur);
        }
        log.info("[CurrencyRates] Tasas de respaldo aplicadas para: {}", FALLBACK_EUR_TO_X.keySet());
    }

    /**
     * Crea o actualiza un par de monedas.
     * Solo escribe en BD si el par no existe o la tasa ha cambiado.
     */
    private void upsertRate(String from, String to, BigDecimal rate) {
        currencyRateRepository.findByFromCurrencyAndToCurrency(from, to)
                .ifPresentOrElse(
                    existing -> {
                        if (existing.getRate().compareTo(rate) != 0) {
                            existing.setRate(rate);
                            currencyRateRepository.save(existing);
                            log.debug("[CurrencyRates] Tasa actualizada: {}/{} = {}", from, to, rate);
                        }
                    },
                    () -> {
                        currencyRateRepository.save(new CurrencyRate(from, to, rate));
                        log.debug("[CurrencyRates] Tasa creada: {}/{} = {}", from, to, rate);
                    }
                );
    }

    /**
     * DTO para deserializar la respuesta de la API Frankfurter.
     * Ejemplo: {"base":"EUR","date":"2025-01-15","rates":{"USD":1.034,"GBP":0.841,...}}
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    public record FrankfurterResponse(String base, String date, Map<String, Double> rates) {}
}
