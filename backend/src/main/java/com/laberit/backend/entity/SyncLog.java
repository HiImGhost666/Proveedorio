package com.laberit.backend.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(
    name = "sync_logs",
    indexes = {
        @Index(name = "idx_sync_logs_supplier", columnList = "proveedor_id"),
        @Index(name = "idx_sync_logs_status", columnList = "status"),
        @Index(name = "idx_sync_logs_timestamp", columnList = "timestamp")
    }
)
public class SyncLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "proveedor_id", nullable = false)
    private Supplier supplier;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(nullable = false)
    private Long latencyMs;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SyncStatus status;

    private Integer itemsProcessed;

    public enum SyncStatus {
        SUCCESS,
        FAILED,
        PARTIAL_SUCCESS
    }

    @PrePersist
    protected void onCreate() {
        this.timestamp = LocalDateTime.now();
    }
    
    // Constructores, Getters y Setters

    public SyncLog() {}

    public SyncLog(Supplier supplier, Long latencyMs, SyncStatus status, Integer itemsProcessed) {
        this.supplier = supplier;
        this.latencyMs = latencyMs;
        this.status = status;
        this.itemsProcessed = itemsProcessed;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Supplier getSupplier() {
        return supplier;
    }

    public void setSupplier(Supplier supplier) {
        this.supplier = supplier;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public Long getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(Long latencyMs) {
        this.latencyMs = latencyMs;
    }

    public SyncStatus getStatus() {
        return status;
    }

    public void setStatus(SyncStatus status) {
        this.status = status;
    }

    public Integer getItemsProcessed() {
        return itemsProcessed;
    }

    public void setItemsProcessed(Integer itemsProcessed) {
        this.itemsProcessed = itemsProcessed;
    }
}
