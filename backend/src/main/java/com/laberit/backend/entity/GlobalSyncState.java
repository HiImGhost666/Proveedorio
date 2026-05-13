package com.laberit.backend.entity;

import com.laberit.backend.dtos.SyncStatus;
import jakarta.persistence.*;
import java.time.LocalDateTime;

/**
 * Single-row table (id=1) that acts as a distributed lock shared across all
 * backend replicas. Replaces the in-memory volatile field in SyncStateService.
 *
 * This way every replica reads/writes the same status, so:
 * - nginx can route freely (round-robin, no ip_hash needed)
 * - only ONE replica runs a sync at a time (the one that wins the CAS in setStatusIfNotInProgress)
 * - all replicas serve consistent sync-status responses
 */
@Entity
@Table(name = "global_sync_state")
public class GlobalSyncState {

    @Id
    private Long id = 1L;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SyncStatus status = SyncStatus.IDLE;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public SyncStatus getStatus() { return status; }
    public void setStatus(SyncStatus status) { this.status = status; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
