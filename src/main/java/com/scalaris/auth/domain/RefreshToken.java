package com.scalaris.auth.domain;

import jakarta.persistence.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "refresh_token",
        indexes = {
                @Index(name = "ix_refresh_user", columnList = "user_id"),
                @Index(name = "ix_refresh_expires", columnList = "expires_at")
        })
public class RefreshToken {

    @Id
    private UUID id; // jti del refresh JWT

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "expires_at", nullable = false)
    private OffsetDateTime expiresAt;

    @Column(name = "revoked_at")
    private OffsetDateTime revokedAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    protected RefreshToken() {}

    public RefreshToken(UUID id, UUID userId, OffsetDateTime expiresAt) {
        this.id = id;
        this.userId = userId;
        this.expiresAt = expiresAt;
    }

    @PrePersist
    void onCreate() {
        this.createdAt = OffsetDateTime.now();
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public OffsetDateTime getExpiresAt() { return expiresAt; }
    public OffsetDateTime getRevokedAt() { return revokedAt; }

    public boolean isRevoked() { return revokedAt != null; }
    public boolean isExpired() { return OffsetDateTime.now().isAfter(expiresAt); }

    public void revokeNow() { this.revokedAt = OffsetDateTime.now(); }
}
