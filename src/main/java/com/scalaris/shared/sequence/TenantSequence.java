// ============================================================================
// shared/sequence/TenantSequence.java
// ============================================================================
package com.scalaris.shared.sequence;

import com.scalaris.shared.base.AuditedTenantEntity;
import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(
        name = "tenant_sequence",
        uniqueConstraints = @UniqueConstraint(name = "uk_seq_tenant_key", columnNames = {"tenant_id", "seq_key"})
)
public class TenantSequence extends AuditedTenantEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "seq_key", nullable = false, length = 40)
    private SequenceKey key;

    @Column(name = "next_value", nullable = false)
    private long nextValue;

    protected TenantSequence() {}

    public static TenantSequence start(UUID tenantId, UUID actorId, SequenceKey key, long startAt) {
        TenantSequence s = new TenantSequence();
        s.key = key;
        s.nextValue = startAt;
        s.assignTenant(tenantId);     // protected: OK (estás dentro del subclass)
        s.auditCreate(actorId);       // protected: OK
        return s;
    }

    public SequenceKey getKey() { return key; }

    public long nextAndIncrement() {
        long v = nextValue;
        nextValue = nextValue + 1;
        return v;
    }
}
