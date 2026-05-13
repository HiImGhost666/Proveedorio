package com.laberit.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Tabla de tasas de cambio entre monedas.
 * Permite normalizar precios de distintos proveedores a una moneda base
 * antes de agregar KPIs globales de coste.
 *
 * Ejemplo de registros semilla:
 *   USD → EUR  ≈ 0.92
 *   GBP → EUR  ≈ 1.17
 *   EUR → EUR  = 1.00  (identidad, para simplificar queries)
 */
@Entity
@Table(
    name = "currency_rates",
    uniqueConstraints = {
        @UniqueConstraint(name = "uq_currency_pair", columnNames = {"from_currency", "to_currency"})
    },
    indexes = {
        @Index(name = "idx_currency_from", columnList = "from_currency")
    }
)
public class CurrencyRate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** Codigo ISO-4217 de la moneda origen (ej: "USD", "EUR", "GBP"). */
    @NotBlank
    @Column(name = "from_currency", nullable = false, length = 3)
    private String fromCurrency;

    /** Codigo ISO-4217 de la moneda destino (moneda base del sistema). */
    @NotBlank
    @Column(name = "to_currency", nullable = false, length = 3)
    private String toCurrency;

    /**
     * Factor de conversion: 1 unidad de from_currency = rate unidades de to_currency.
     * Precision 18,6 para tasas de cambio de alta precision.
     */
    @NotNull
    @Positive
    @Column(name = "rate", nullable = false, precision = 18, scale = 6)
    private BigDecimal rate;

    /** Marca de tiempo de la ultima actualizacion de la tasa. */
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onSave() {
        this.updatedAt = LocalDateTime.now();
    }

    public CurrencyRate() {}

    public CurrencyRate(String fromCurrency, String toCurrency, BigDecimal rate) {
        this.fromCurrency = fromCurrency;
        this.toCurrency = toCurrency;
        this.rate = rate;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getFromCurrency() { return fromCurrency; }
    public void setFromCurrency(String fromCurrency) { this.fromCurrency = fromCurrency; }

    public String getToCurrency() { return toCurrency; }
    public void setToCurrency(String toCurrency) { this.toCurrency = toCurrency; }

    public BigDecimal getRate() { return rate; }
    public void setRate(BigDecimal rate) { this.rate = rate; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
