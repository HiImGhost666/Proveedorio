package com.proveedorio.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Registro historico de cambios de stock por par producto/proveedor.
 * Se inserta unicamente cuando el stock cambia respecto al valor anterior,
 * lo que permite detectar patrones de volatilidad en ventanas de 24 horas.
 */
@Entity
@Table(
    name = "stock_history",
    indexes = {
        @Index(name = "idx_sh_product_supplier_time", columnList = "producto_id, proveedor_id, recorded_at")
    }
)
public class StockHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private MasterProduct masterProduct;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proveedor_id", nullable = false)
    private Supplier supplier;

    @Column(name = "stock_value", nullable = false)
    private Integer stockValue;

    @Column(name = "recorded_at", nullable = false)
    private LocalDateTime recordedAt;

    @PrePersist
    protected void onCreate() {
        if (this.recordedAt == null) {
            this.recordedAt = LocalDateTime.now();
        }
    }

    public StockHistory() {}

    public StockHistory(MasterProduct masterProduct, Supplier supplier, Integer stockValue) {
        this.masterProduct = masterProduct;
        this.supplier = supplier;
        this.stockValue = stockValue;
        this.recordedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public MasterProduct getMasterProduct() { return masterProduct; }
    public void setMasterProduct(MasterProduct masterProduct) { this.masterProduct = masterProduct; }
    public Supplier getSupplier() { return supplier; }
    public void setSupplier(Supplier supplier) { this.supplier = supplier; }
    public Integer getStockValue() { return stockValue; }
    public void setStockValue(Integer stockValue) { this.stockValue = stockValue; }
    public LocalDateTime getRecordedAt() { return recordedAt; }
    public void setRecordedAt(LocalDateTime recordedAt) { this.recordedAt = recordedAt; }
}
