package com.laberit.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;

import java.time.LocalDateTime;

/**
 * Entidad de mapeo campo interno ↔ externo para normalización de respuestas de APIs de proveedores.
 * Tabla provider_mapping; columnas en español (campo_interno, campo_externo, tipo_transformacion).
 */
@Entity
@Table(name = "provider_mapping")
public class ProviderMapping {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer mappingId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proveedor_id", nullable = false)
    private Supplier supplier;

    @NotBlank(message = "El campo interno del mapeo es obligatorio")
    @Column(name = "campo_interno", nullable = false)
    private String internalField;

    @NotBlank(message = "El campo externo del mapeo es obligatorio")
    @Column(name = "campo_externo", nullable = false)
    private String externalField;

    @Column(name = "tipo_transformacion")
    private String transformationType;

    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public ProviderMapping(){}

    public ProviderMapping(Supplier supplier, String internalField, String externalField, String transformationType) {
        this.supplier = supplier;
        this.internalField = internalField;
        this.externalField = externalField;
        this.transformationType = transformationType;
    }

    public Integer getProveedorId() {
        return supplier != null ? supplier.getId() : null;
    }

    public Supplier getSupplier() {
        return supplier;
    }

    public void setSupplier(Supplier supplier) {
        this.supplier = supplier;
    }

    public Integer getMappingId() {
        return mappingId;
    }

    public String getInternalField() {
        return internalField;
    }

    public void setInternalField(String internalField) {
        this.internalField = internalField;
    }

    public String getExternalField() {
        return externalField;
    }

    public void setExternalField(String externalField) {
        this.externalField = externalField;
    }

    public String getTransformationType() {
        return transformationType;
    }

    public void setTransformationType(String transformationType) {
        this.transformationType = transformationType;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
