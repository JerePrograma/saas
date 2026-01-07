// ============================================================================
// shared/base/AuditedEntity.java
// ============================================================================
package com.scalaris.shared.base;

import jakarta.persistence.*;
import org.hibernate.annotations.UuidGenerator;

import java.time.OffsetDateTime;
import java.util.UUID;

@MappedSuperclass
public abstract class AuditedEntity {

    @Id
    @UuidGenerator
    @Column(columnDefinition = "uuid", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "created_by", columnDefinition = "uuid", updatable = false)
    private UUID createdBy;

    @Column(name = "updated_by", columnDefinition = "uuid")
    private UUID updatedBy;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected AuditedEntity() {}

    public UUID getId() { return id; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public UUID getCreatedBy() { return createdBy; }
    public UUID getUpdatedBy() { return updatedBy; }
    public boolean isActive() { return active; }
    public long getVersion() { return version; }

    protected void auditCreate(UUID actorId) {
        this.createdBy = actorId;
        this.updatedBy = actorId;
    }

    protected void auditUpdate(UUID actorId) {
        this.updatedBy = actorId;
    }

    public void deactivate(UUID actorId) {
        this.active = false;
        auditUpdate(actorId);
    }

    @PrePersist
    protected void onCreate() {
        OffsetDateTime now = OffsetDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = OffsetDateTime.now();
    }
}
