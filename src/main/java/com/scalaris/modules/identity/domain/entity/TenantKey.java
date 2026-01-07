package com.scalaris.modules.identity.domain.entity;

import com.scalaris.shared.base.AuditedEntity;
import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(
        name = "tenant_key",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_tenant_key_type_value", columnNames = {"key_type", "key_value"}),
                @UniqueConstraint(name = "uk_tenant_key_tenant_type", columnNames = {"tenant_id", "key_type"})
        },
        indexes = @Index(name = "ix_tenant_key_tenant", columnList = "tenant_id")
)
public class TenantKey extends AuditedEntity {

    @Column(name = "tenant_id", columnDefinition = "uuid", nullable = false, updatable = false)
    private UUID tenantId;

    @Column(name = "key_type", nullable = false, length = 30)
    private String keyType = "SLUG";

    @Column(name = "key_value", nullable = false, length = 120)
    private String keyValue;

    protected TenantKey() {}

    public static TenantKey create(UUID actorId, UUID tenantId, String keyType, String keyValue) {
        if (tenantId == null) throw new IllegalArgumentException("tenantId requerido");
        if (keyValue == null || keyValue.trim().isBlank()) throw new IllegalArgumentException("keyValue requerido");

        TenantKey k = new TenantKey();
        k.auditCreate(actorId);
        k.tenantId = tenantId;
        k.keyType = (keyType == null || keyType.isBlank()) ? "SLUG" : keyType.trim();
        k.keyValue = keyValue.trim();
        return k;
    }

    public UUID getTenantId() { return tenantId; }
    public String getKeyType() { return keyType; }
    public String getKeyValue() { return keyValue; }
}
