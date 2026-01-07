// ============================================================================
// modules/identity/domain/entity/PasswordResetToken.java
// ============================================================================
package com.scalaris.modules.identity.domain.entity;

import com.scalaris.shared.base.AuditedTenantEntity;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "password_reset_token",
        indexes = {
                @Index(name = "ix_prt_user", columnList = "user_id"),
                @Index(name = "ix_prt_expires", columnList = "expires_at")
        }
)
public class PasswordResetToken extends AuditedTenantEntity {

    public enum Purpose { RESET, INVITE }

    @Column(name = "user_id", columnDefinition = "uuid", nullable = false)
    private UUID userId;

    @Column(name = "token_hash", nullable = false, length = 200)
    private String tokenHash;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "used_at")
    private OffsetDateTime usedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false, length = 20)
    private Purpose purpose = Purpose.RESET;

    protected PasswordResetToken() {}

    public static PasswordResetToken issue(UUID tenantId,
                                           UUID actorId,
                                           UUID userId,
                                           Purpose purpose,
                                           String tokenHash,
                                           OffsetDateTime expiresAt) {
        if (tenantId == null) throw new IllegalArgumentException("tenantId requerido");
        if (userId == null) throw new IllegalArgumentException("userId requerido");
        if (tokenHash == null || tokenHash.isBlank()) throw new IllegalArgumentException("tokenHash requerido");
        if (expiresAt == null) throw new IllegalArgumentException("expiresAt requerido");

        PasswordResetToken t = new PasswordResetToken();
        t.assignTenant(tenantId);
        t.auditCreate(actorId);

        t.userId = userId;
        t.purpose = (purpose == null) ? Purpose.RESET : purpose;
        t.tokenHash = tokenHash;
        t.expiresAt = expiresAt;
        return t;
    }

    public UUID getUserId() { return userId; }
    public String getTokenHash() { return tokenHash; }
    public OffsetDateTime getExpiresAt() { return expiresAt; }
    public OffsetDateTime getUsedAt() { return usedAt; }
    public Purpose getPurpose() { return purpose; }

    public boolean isUsableAt(OffsetDateTime now) {
        return usedAt == null && expiresAt.isAfter(now);
    }

    public void markUsed(OffsetDateTime now, UUID actorId) {
        this.usedAt = now;
        auditUpdate(actorId);
    }
}
