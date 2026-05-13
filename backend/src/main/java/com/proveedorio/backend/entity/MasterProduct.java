package com.proveedorio.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad que representa el producto maestro (canónico) en el sistema.
 * Agrupa la información central de un producto que puede estar disponible en varios proveedores.
 * Se persiste en la tabla {@code producto_maestro}; las relaciones con ofertas por proveedor
 * se gestionan mediante {@link ProductSupplier}.
 */
@Entity
@Table(
    name = "producto_maestro",
    indexes = {
        @Index(name = "idx_producto_maestro_mpn", columnList = "mpn"),
        @Index(name = "idx_producto_maestro_brand", columnList = "brand"),
        @Index(name = "idx_producto_maestro_model", columnList = "model"),
        @Index(name = "idx_producto_maestro_fecha_actualizacion", columnList = "fecha_actualizacion"),
        @Index(name = "idx_producto_maestro_category", columnList = "category"),
        @Index(name = "idx_producto_maestro_brand_model_mpn", columnList = "brand, model, mpn")
    }
)
public class MasterProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull
    @Column(name = "mpn", unique = true, nullable = false)
    private String mpn;

    @NotNull
    @Size(min = 1, max = 255)
    @Column(nullable = false)
    private String brand;

    @NotNull
    @Size(min = 1, max = 255)
    @Column(nullable = false)
    private String model;

    /**
     * Categoría funcional del producto (por ejemplo: PC, impresora, móvil).
     * Es un atributo canónico a nivel de producto maestro, independiente del proveedor.
     */
    @Column(name = "category", length = 255)
    private String category;

    @Column(name = "tech_specs", columnDefinition = "TEXT")
    private String techSpecs;

    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime fechaActualizacion;

    @OneToMany(mappedBy = "masterProduct", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductSupplier> productSuppliers = new ArrayList<>();

    @OneToMany(orphanRemoval = true, mappedBy = "masterProduct")
    private List<PriceHistory> priceHistoryList = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        fechaCreacion = LocalDateTime.now();
        fechaActualizacion = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        fechaActualizacion = LocalDateTime.now();
    }

    public MasterProduct() {}

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getMpn() {
        return mpn;
    }

    public void setMpn(String mpn) {
        this.mpn = mpn;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getTechSpecs() {
        return techSpecs;
    }

    public void setTechSpecs(String techSpecs) {
        this.techSpecs = techSpecs;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public LocalDateTime getFechaActualizacion() {
        return fechaActualizacion;
    }

    public List<ProductSupplier> getProductSuppliers() {
        return productSuppliers;
    }

    public void setProductSuppliers(List<ProductSupplier> productSuppliers) {
        this.productSuppliers = productSuppliers;
    }

    public List<PriceHistory> getPriceHistoryList() {
        return priceHistoryList;
    }

    public void setPriceHistoryList(List<PriceHistory> priceHistoryList) {
        this.priceHistoryList = priceHistoryList;
    }
}
