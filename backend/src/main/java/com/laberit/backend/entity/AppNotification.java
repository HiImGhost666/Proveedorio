package com.laberit.backend.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "notificaciones_globales",
        indexes = {
                @Index(name = "idx_notif_created_at", columnList = "created_at"),
                @Index(name = "idx_notif_expires_at", columnList = "expires_at"),
                @Index(name = "idx_notif_type", columnList = "type"),
                @Index(name = "idx_notif_dedup_key", columnList = "dedup_key")
        }
)
public class AppNotification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 32)
    private AppNotificationType type;

    @Column(name = "title", nullable = false, length = 150)
    private String title;

    @Column(name = "message", nullable = false, length = 1000)
    private String message;

    @Column(name = "dedup_key", length = 255)
    private String dedupKey;

    @Column(name = "action_path", length = 255)
    private String actionPath;

    @Column(name = "scope_tag", length = 40)
    private String scopeTag;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id")
    private MasterProduct masterProduct;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @OneToMany(mappedBy = "notification", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserNotificationStatus> userStatuses = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        LocalDateTime nowUtc = LocalDateTime.now(ZoneOffset.UTC);
        if (this.createdAt == null) {
            this.createdAt = nowUtc;
        }
        if (this.expiresAt == null) {
            this.expiresAt = nowUtc.plusHours(24);
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AppNotificationType getType() {
        return type;
    }

    public void setType(AppNotificationType type) {
        this.type = type;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getDedupKey() {
        return dedupKey;
    }

    public void setDedupKey(String dedupKey) {
        this.dedupKey = dedupKey;
    }

    public String getActionPath() {
        return actionPath;
    }

    public void setActionPath(String actionPath) {
        this.actionPath = actionPath;
    }

    public String getScopeTag() {
        return scopeTag;
    }

    public void setScopeTag(String scopeTag) {
        this.scopeTag = scopeTag;
    }

    public Supplier getSupplier() {
        return supplier;
    }

    public void setSupplier(Supplier supplier) {
        this.supplier = supplier;
    }

    public MasterProduct getMasterProduct() {
        return masterProduct;
    }

    public void setMasterProduct(MasterProduct masterProduct) {
        this.masterProduct = masterProduct;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public List<UserNotificationStatus> getUserStatuses() {
        return userStatuses;
    }

    public void setUserStatuses(List<UserNotificationStatus> userStatuses) {
        this.userStatuses = userStatuses;
    }
}
