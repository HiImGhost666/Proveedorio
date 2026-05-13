package com.laberit.backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Entidad que representa a un proveedor de productos.
 * Contiene información de conexión a su API y configuraciones de mapeo.
 * La tabla y columnas en base de datos se mantienen en español (proveedores, nombre, activo, etc.).
 */
@Entity
@Table(name = "proveedores")
public class Supplier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotBlank(message = "El nombre del proveedor es obligatorio")
    @Column(name = "nombre", unique = true, nullable = false, length = 100)
    private String name;

    @NotBlank(message = "La URL base de la API es obligatoria")
    @Column(nullable = false)
    private String baseUrlApi;

    @Column(name = "contacto", length = 100)
    private String contact;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "horario", length = 100)
    private String schedule;

    /** Número de teléfono de contacto del proveedor. */
    @Column(name = "telefono", length = 30)
    private String phone;

    /** Sitio web del proveedor. */
    @Column(name = "website", length = 255)
    private String website;

    /** País de origen del proveedor (ISO 3166-1 alpha-2, ej: "ES", "US", "DE"). */
    @Column(name = "pais", length = 3)
    private String country;

    /**
     * Moneda predeterminada que usa este proveedor en sus precios (ISO 4217, ej: "EUR", "USD").
     * Coincide con el campo {@code currency} de {@link ProductSupplier}.
     * Se usa en la vista de detalle y para contextualizar los KPIs de precio.
     */
    @Column(name = "moneda_defecto", length = 3)
    private String defaultCurrency;

    @NotBlank(message = "La API Key es obligatoria")
    @Column(name = "ApiKey", nullable = false, length = 255)
    private String apiKey;

    @Column(name = "endpoint_catalogo")
    private String catalogEndpoint;

    @Column(name = "endpoint_detalle")
    private String detailEndpoint;

    @Column(name = "endpoint_busqueda")
    private String searchEndpoint;

    @Column(name = "soporta_sincronizacion_masiva")
    private Boolean supportsBulkSync;

    @NotNull(message = "El estado activo del proveedor debe estar definido")
    @Column(name = "activo")
    private Boolean active;

    @Column(name = "fecha_creacion", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "fecha_actualizacion")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "supplier", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ProductSupplier> productSuppliers = new ArrayList<>();

    @OneToMany(mappedBy = "supplier", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<ProviderMapping> mappingConfigurations = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.active = true;
        if (this.catalogEndpoint == null) this.catalogEndpoint = "/products";
        if (this.detailEndpoint == null) this.detailEndpoint = "/products/{sku}";
        if (this.searchEndpoint == null) this.searchEndpoint = "/products?q={query}";
        if (this.supportsBulkSync == null) this.supportsBulkSync = true;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getBaseUrlApi() {
        return baseUrlApi;
    }

    public void setBaseUrlApi(String baseUrlApi) {
        this.baseUrlApi = baseUrlApi;
    }

    public String getContact() {
        return contact;
    }

    public void setContact(String contact) {
        this.contact = contact;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getSchedule() {
        return schedule;
    }

    public void setSchedule(String schedule) {
        this.schedule = schedule;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getCatalogEndpoint() {
        return catalogEndpoint;
    }

    public void setCatalogEndpoint(String catalogEndpoint) {
        this.catalogEndpoint = catalogEndpoint;
    }

    public String getDetailEndpoint() {
        return detailEndpoint;
    }

    public void setDetailEndpoint(String detailEndpoint) {
        this.detailEndpoint = detailEndpoint;
    }

    public String getSearchEndpoint() {
        return searchEndpoint;
    }

    public void setSearchEndpoint(String searchEndpoint) {
        this.searchEndpoint = searchEndpoint;
    }

    public Boolean getSupportsBulkSync() {
        return supportsBulkSync;
    }

    public void setSupportsBulkSync(Boolean supportsBulkSync) {
        this.supportsBulkSync = supportsBulkSync;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
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

    public List<ProductSupplier> getProductSuppliers() {
        return productSuppliers;
    }

    public void setProductSuppliers(List<ProductSupplier> productSuppliers) {
        this.productSuppliers = productSuppliers;
    }

    public List<ProviderMapping> getMappingConfigurations() {
        return mappingConfigurations;
    }

    public void setMappingConfigurations(List<ProviderMapping> mappingConfigurations) {
        this.mappingConfigurations = mappingConfigurations;
    }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getWebsite() { return website; }
    public void setWebsite(String website) { this.website = website; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public String getDefaultCurrency() { return defaultCurrency; }
    public void setDefaultCurrency(String defaultCurrency) { this.defaultCurrency = defaultCurrency; }
}
