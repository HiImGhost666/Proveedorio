package com.laberit.backend.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Tabla intermedia que conecta Producto con Proveedor.
 * Almacena los datos variables: stock, precio, moneda e ID interno del proveedor.
 * Reemplaza a la antigua StockCache.
 */
@Entity
@Table(
    name = "product_supplier",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"producto_id", "proveedor_id"})
    },
    indexes = {
        @Index(name = "idx_product_supplier_producto", columnList = "producto_id"),
        @Index(name = "idx_product_supplier_proveedor", columnList = "proveedor_id"),
        @Index(name = "idx_product_supplier_stock", columnList = "stock")
    }
)
public class ProductSupplier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "producto_id", nullable = false)
    private MasterProduct masterProduct;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proveedor_id", nullable = false)
    private Supplier supplier;

    @Column(name = "external_provider_id")
    private String externalProviderId; // ID que el proveedor usa internamente

    // --- CAMPOS COMERCIALES Y LOGÍSTICOS ---
    
    /**
     * El precio de coste o "precio partner" que tú pagas al proveedor.
     */
    @Column(name = "price", nullable = false)
    private BigDecimal price;

    /**
     * El Precio de Venta al Público Recomendado (PVP / MSRP).
     * Este es el precio que el proveedor sugiere que uses para el cliente final.
     */
    @Column(name = "retail_price")
    private BigDecimal retailPrice;

    /**
     * Código de barras universal (EAN-13). Esencial para unificar productos.
     */
    @Column(name = "ean", length = 13)
    private String ean;

    /**
     * Cantidad mínima de pedido (Minimum Order Quantity).
     */
    @Column(name = "moq")
    private Integer moq;

    /**
     * Condición del producto (Nuevo, Reacondicionado, etc.).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "product_condition")
    private ProductCondition productCondition;

    /**
     * Moneda ISO-4217 en la que el proveedor reporta el precio (ej: "EUR", "USD", "GBP").
     * Permite normalizar precios a la moneda base del sistema antes de agregar KPIs globales.
     * Valor por defecto "EUR" para compatibilidad con registros existentes.
     */
    @Column(name = "currency", nullable = false, length = 3)
    private String currency = "EUR";

    // --- FIN NUEVOS CAMPOS ---

    @Column(name = "stock", nullable = false)
    private Integer stock;

    @Column(name = "last_updated", nullable = false)
    private LocalDateTime lastUpdated;

    @Column(name = "is_available", nullable = false)
    private Boolean isAvailable;

    @Column(name = "data_stale", nullable = false)
    private Boolean dataStale; // Equivalente a datosDesactualizados

    @Column(name = "last_error")
    private String lastError;

    public enum ProductCondition {
        NEW,
        BOX_DAMAGED,
        REFURBISHED,
        USED
    }

    @PrePersist
    protected void onCreate() {
        this.lastUpdated = LocalDateTime.now();
        if (this.dataStale == null) this.dataStale = false;
        if (this.isAvailable == null) this.isAvailable = true;
        if (this.productCondition == null) this.productCondition = ProductCondition.NEW;
    }

    @PreUpdate
    protected void onUpdate() {
        this.lastUpdated = LocalDateTime.now();
    }

    /**
     * Verifica si los datos son recientes (menos de 3 minutos).
     */
    public boolean isFresh() {
        return !Boolean.TRUE.equals(dataStale) &&
                lastUpdated.isAfter(LocalDateTime.now().minusMinutes(3));
    }

    public void markAsStale() {
        this.dataStale = true;
    }

    public void markAsFresh() {
        this.dataStale = false;
        this.lastError = null;
    }

    public void registerError(String message) {
        this.lastError = message;
        this.dataStale = true;
    }

    public ProductSupplier() {}

    // Getters y Setters

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
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

    public String getExternalProviderId() {
        return externalProviderId;
    }

    public void setExternalProviderId(String externalProviderId) {
        this.externalProviderId = externalProviderId;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getRetailPrice() {
        return retailPrice;
    }

    public void setRetailPrice(BigDecimal retailPrice) {
        this.retailPrice = retailPrice;
    }

    public String getEan() {
        return ean;
    }

    public void setEan(String ean) {
        this.ean = ean;
    }

    public Integer getMoq() {
        return moq;
    }

    public void setMoq(Integer moq) {
        this.moq = moq;
    }

    public ProductCondition getProductCondition() {
        return productCondition;
    }

    public void setProductCondition(ProductCondition productCondition) {
        this.productCondition = productCondition;
    }

    public Integer getStock() {
        return stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public Boolean getAvailable() {
        return isAvailable;
    }

    public void setAvailable(Boolean available) {
        isAvailable = available;
    }

    public Boolean getDataStale() {
        return dataStale;
    }

    public void setDataStale(Boolean dataStale) {
        this.dataStale = dataStale;
    }

    public String getLastError() {
        return lastError;
    }

    public void setLastError(String lastError) {
        this.lastError = lastError;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency != null ? currency.toUpperCase() : "EUR";
    }
}
