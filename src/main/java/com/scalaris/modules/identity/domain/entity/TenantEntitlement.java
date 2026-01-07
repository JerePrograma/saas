    package com.scalaris.modules.identity.domain.entity;

    import com.scalaris.shared.base.AuditedTenantEntity;
    import jakarta.persistence.*;
    import org.hibernate.annotations.ColumnTransformer;

    import java.util.UUID;

    @Entity
    @Table(
            name = "tenant_entitlement",
            uniqueConstraints = @UniqueConstraint(name = "uk_tenant_ent_module", columnNames = {"tenant_id", "module_code"}),
            indexes = @Index(name = "ix_tenant_ent_tenant", columnList = "tenant_id")
    )
    public class TenantEntitlement extends AuditedTenantEntity {

        @Column(name = "module_code", nullable = false, length = 60)
        private String moduleCode;

        @Column(name = "enabled", nullable = false)
        private boolean enabled;

        @Column(name = "limits_json", nullable = false, columnDefinition = "jsonb")
        @ColumnTransformer(write = "?::jsonb")
        private String limitsJson = "{}";

        protected TenantEntitlement() {}

        public static TenantEntitlement of(UUID tenantId, UUID actorId, String moduleCode, boolean enabled, String limitsJson) {
            if (tenantId == null) throw new IllegalArgumentException("tenantId requerido");
            if (moduleCode == null || moduleCode.trim().isBlank()) throw new IllegalArgumentException("moduleCode requerido");

            TenantEntitlement e = new TenantEntitlement();
            e.assignTenant(tenantId);
            e.auditCreate(actorId);

            e.moduleCode = moduleCode.trim();
            e.enabled = enabled;
            e.limitsJson = (limitsJson == null || limitsJson.isBlank()) ? "{}" : limitsJson.trim();
            return e;
        }

        public String getModuleCode() { return moduleCode; }
        public boolean isEnabled() { return enabled; }
        public String getLimitsJson() { return limitsJson; }

        public void setEnabled(UUID actorId, boolean enabled) {
            this.enabled = enabled;
            auditUpdate(actorId);
        }

        public void setLimitsJson(UUID actorId, String limitsJson) {
            this.limitsJson = (limitsJson == null || limitsJson.isBlank()) ? "{}" : limitsJson.trim();
            auditUpdate(actorId);
        }
    }
