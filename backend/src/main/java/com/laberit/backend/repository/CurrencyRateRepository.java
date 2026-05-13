package com.laberit.backend.repository;

import com.laberit.backend.entity.CurrencyRate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repositorio para tasas de cambio entre monedas.
 * Usado por SupplierAnalyticsService para normalizar precios
 * de proveedores que operen en distintas divisas.
 */
@Repository
public interface CurrencyRateRepository extends JpaRepository<CurrencyRate, Integer> {

    /**
     * Busca la tasa de conversion directa entre dos monedas.
     *
     * @param fromCurrency moneda origen (ej: "USD")
     * @param toCurrency   moneda destino (ej: "EUR")
     * @return la tasa, o empty si el par no esta registrado
     */
    Optional<CurrencyRate> findByFromCurrencyAndToCurrency(String fromCurrency, String toCurrency);

    boolean existsByFromCurrencyAndToCurrency(String fromCurrency, String toCurrency);
}
