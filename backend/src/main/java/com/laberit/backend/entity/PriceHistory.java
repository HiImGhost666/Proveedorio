package com.laberit.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidad que registra el historial de precios de los productos por proveedor.
 * Permite analizar la tendencia de precios en el tiempo.
 * Tabla y columnas en base de datos en español (historial_precios, precio_costo, fecha_registro).
 */
@Entity
@Table(
    name = "historial_precios",
    indexes = {
        @Index(name = "idx_historial_precios_producto", columnList = "producto_id"),
        @Index(name = "idx_historial_precios_proveedor", columnList = "proveedor_id"),
        @Index(name = "idx_historial_precios_fecha_registro", columnList = "fecha_registro")
    }
)
public class PriceHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "historial_id")
    private Integer historyId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id")
    private MasterProduct masterProduct;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proveedor_id")
    private Supplier supplier;

    @NotNull(message = "El precio de coste no puede ser nulo")
    @Column(name = "precio_costo", precision = 18, scale = 2)
    private BigDecimal costPrice;

    @Column(name = "fecha_registro")
    private LocalDateTime registeredAt;

    @PrePersist
    protected void onCreate() {
        this.registeredAt = LocalDateTime.now();
    }

    public PriceHistory() {}

    public PriceHistory(MasterProduct masterProduct, Supplier supplier, BigDecimal costPrice) {
        this.masterProduct = masterProduct;
        this.supplier = supplier;
        this.costPrice = costPrice;
    }

    /**
     * Calcula la tendencia del precio comparando con un precio anterior.
     * Mensajes en español para la capa de presentación.
     *
     * @param previousPrice precio previo para comparar.
     * @return "ALZA ↑", "BAJA ↓", "ESTABLE =" o "ESTABLE (Sin historial)".
     */
    public String computeTrend(BigDecimal previousPrice) {
        if (previousPrice == null || previousPrice.compareTo(BigDecimal.ZERO) == 0) {
            return "ESTABLE (Sin historial)";
        }
        int cmp = this.costPrice != null ? this.costPrice.compareTo(previousPrice) : 0;
        if (cmp > 0) return "ALZA ↑";
        if (cmp < 0) return "BAJA ↓";
        return "ESTABLE =";
    }

    public Integer getHistoryId() {
        return historyId;
    }

    public void setHistoryId(Integer historyId) {
        this.historyId = historyId;
    }

    public MasterProduct getMasterProduct() {
        return masterProduct;
    }

    public void setMasterProduct(MasterProduct masterProduct) {
        this.masterProduct = masterProduct;
    }

    public Supplier getSupplier() {
        return supplier;
    }

    public void setSupplier(Supplier supplier) {
        this.supplier = supplier;
    }

    public BigDecimal getCostPrice() {
        return costPrice;
    }

    public void setCostPrice(BigDecimal costPrice) {
        this.costPrice = costPrice;
    }

    public LocalDateTime getRegisteredAt() {
        return registeredAt;
    }

    public void setRegisteredAt(LocalDateTime registeredAt) {
        this.registeredAt = registeredAt;
    }
}
