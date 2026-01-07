// ============================================================================
// modules/identity/domain/entity/UserSession.java
// ============================================================================
package com.scalaris.modules.identity.domain.entity;

import com.scalaris.shared.base.AuditedTenantEntity;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Entity
@Table(
        name = "user_session",
        uniqueConstraints = @UniqueConstraint(name = "uk_session_token_hash", columnNames = {"session_token_hash"}),
        indexes = {
                @Index(name = "ix_session_user", columnList = "user_id"),
                @Index(name = "ix_session_expires", columnList = "expires_at"),
                // Si tu base tiene tenant_id (casi seguro):
                @Index(name = "ix_session_tenant_user_rev", columnList = "tenant_id, user_id, revoked_at"),
                @Index(name = "ix_session_tenant_expires_rev", columnList = "tenant_id, expires_at, revoked_at")
        }
)
public class UserSession extends AuditedTenantEntity {

    @Column(name = "user_id", columnDefinition = "uuid", nullable = false)
    private UUID userId;

    @Column(name = "session_token_hash", length = 200, nullable = false)
    private String sessionTokenHash;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "last_seen_at", nullable = false)
    private OffsetDateTime lastSeenAt;

    @Column(name = "revoked_at")
    private OffsetDateTime revokedAt;

    @Column(name = "revoked_by", columnDefinition = "uuid")
    private UUID revokedBy;

    @Column(name = "ip", length = 80)
    private String ip;

    @Column(name = "user_agent", length = 400)
    private String userAgent;

    @Column(name = "security_stamp", columnDefinition = "uuid", nullable = false)
    private UUID securityStamp;

    protected UserSession() {}

    public static UserSession open(UUID tenantId,
                                   UUID actorId,
                                   UUID userId,
                                   UUID securityStamp,
                                   String tokenHash,
                                   OffsetDateTime now,
                                   OffsetDateTime expiresAt,
                                   String ip,
                                   String ua) {
        if (tenantId == null) throw new IllegalArgumentException("tenantId requerido");
        if (userId == null) throw new IllegalArgumentException("userId requerido");
        if (securityStamp == null) throw new IllegalArgumentException("securityStamp requerido");
        if (tokenHash == null || tokenHash.isBlank()) throw new IllegalArgumentException("tokenHash requerido");
        if (now == null || expiresAt == null) throw new IllegalArgumentException("now/expiresAt requeridos");

        UserSession s = new UserSession();
        s.assignTenant(tenantId);
        s.auditCreate(actorId);

        s.userId = userId;
        s.securityStamp = securityStamp;
        s.sessionTokenHash = tokenHash;
        s.lastSeenAt = now;
        s.expiresAt = expiresAt;
        s.ip = ip;
        s.userAgent = ua;

        return s;
    }

    public UUID getUserId() { return userId; }
    public String getSessionTokenHash() { return sessionTokenHash; }
    public OffsetDateTime getExpiresAt() { return expiresAt; }
    public OffsetDateTime getLastSeenAt() { return lastSeenAt; }
    public OffsetDateTime getRevokedAt() { return revokedAt; }
    public UUID getRevokedBy() { return revokedBy; }
    public UUID getSecurityStamp() { return securityStamp; }

    public boolean isActiveAt(OffsetDateTime now) {
        return revokedAt == null && expiresAt.isAfter(now);
    }

    public boolean matchesUserStamp(UUID currentUserStamp) {
        return currentUserStamp != null && currentUserStamp.equals(this.securityStamp);
    }

    /** Evita escribir en DB por cada request (throttle). */
    public boolean touchIfStale(OffsetDateTime now, UUID actorId, long minMinutesBetweenTouches) {
        if (now == null) return false;
        if (revokedAt != null) return false;

        OffsetDateTime threshold = lastSeenAt.plus(minMinutesBetweenTouches, ChronoUnit.MINUTES);
        if (threshold.isAfter(now)) return false;

        this.lastSeenAt = now;
        auditUpdate(actorId);
        return true;
    }

    public void revoke(OffsetDateTime now, UUID actorId) {
        if (this.revokedAt != null) return; // idempotente
        this.revokedAt = now;
        this.revokedBy = actorId;
        auditUpdate(actorId);
    }
}
