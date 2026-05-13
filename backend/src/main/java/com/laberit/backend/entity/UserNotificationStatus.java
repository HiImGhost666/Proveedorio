package com.laberit.backend.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Entity
@Table(
        name = "notificacion_usuario_estado",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_notif_user_read", columnNames = {"notification_id", "usuario_id"})
        },
        indexes = {
                @Index(name = "idx_notif_user", columnList = "usuario_id"),
                @Index(name = "idx_notif_notification", columnList = "notification_id")
        }
)
public class UserNotificationStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "notification_id", nullable = false)
    private AppNotification notification;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private User user;

    @Column(name = "read_at", nullable = false)
    private LocalDateTime readAt;

    @PrePersist
    protected void onCreate() {
        if (this.readAt == null) {
            this.readAt = LocalDateTime.now(ZoneOffset.UTC);
        }
    }

    public UserNotificationStatus() {
    }

    public UserNotificationStatus(AppNotification notification, User user) {
        this.notification = notification;
        this.user = user;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public AppNotification getNotification() {
        return notification;
    }

    public void setNotification(AppNotification notification) {
        this.notification = notification;
    }

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public LocalDateTime getReadAt() {
        return readAt;
    }

    public void setReadAt(LocalDateTime readAt) {
        this.readAt = readAt;
    }
}
