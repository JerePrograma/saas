// ============================================================================
// modules/identity/domain/entity/Tenant.java
// ============================================================================
package com.scalaris.modules.identity.domain.entity;

import com.scalaris.shared.base.AuditedEntity;
import jakarta.persistence.*;
import org.hibernate.annotations.ColumnTransformer;

@Entity
@Table(
        name = "tenant",
        indexes = {
                @Index(name = "ix_tenant_status", columnList = "status")
        }
)
public class Tenant extends AuditedEntity {

    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private TenantStatus status = TenantStatus.ACTIVE;

    @Enumerated(EnumType.STRING)
    @Column(name = "plan", nullable = false, length = 30)
    private TenantPlan plan = TenantPlan.BASIC;

    @Column(name="settings_json", columnDefinition="jsonb", nullable=false)
    @ColumnTransformer(write = "?::jsonb")
    private String settingsJson;

    public enum TenantStatus { ACTIVE, SUSPENDED, CANCELED }
    public enum TenantPlan { BASIC, PRO, ENTERPRISE }

    protected Tenant() {}

    public static Tenant create(java.util.UUID actorId, String name, TenantPlan plan, String settingsJson) {
        Tenant t = new Tenant();
        t.name = requireName(name);
        t.plan = plan == null ? TenantPlan.BASIC : plan;
        t.settingsJson = normalizeSettings(settingsJson);

        // asumimos AuditedEntity tiene auditCreate(actorId) protected (igual que tu AuditedTenantEntity)
        t.auditCreate(actorId);
        return t;
    }

    public void update(java.util.UUID actorId, String name, String settingsJson) {
        this.name = requireName(name);
        this.settingsJson = normalizeSettings(settingsJson);
        auditUpdate(actorId);
    }

    public void changeStatus(java.util.UUID actorId, TenantStatus newStatus) {
        if (newStatus == null) return;
        this.status = newStatus;
        auditUpdate(actorId);
    }

    public void changePlan(java.util.UUID actorId, TenantPlan newPlan) {
        if (newPlan == null) return;
        this.plan = newPlan;
        auditUpdate(actorId);
    }

    public String getName() { return name; }
    public TenantStatus getStatus() { return status; }
    public TenantPlan getPlan() { return plan; }
    public String getSettingsJson() { return settingsJson; }

    private static String requireName(String name) {
        if (name == null) throw new IllegalArgumentException("name requerido");
        String n = name.trim();
        if (n.isBlank()) throw new IllegalArgumentException("name requerido");
        return n;
    }

    private static String normalizeSettings(String json) {
        if (json == null || json.trim().isBlank()) return "{}";
        return json.trim();
    }
}
