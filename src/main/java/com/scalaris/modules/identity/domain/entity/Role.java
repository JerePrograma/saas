package com.scalaris.modules.identity.domain.entity;

import com.scalaris.shared.base.AuditedTenantEntity;
import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(
        name = "role",
        uniqueConstraints = @UniqueConstraint(name = "uk_role_tenant_name", columnNames = {"tenant_id", "name"})
)
public class Role extends AuditedTenantEntity {

    @Column(name = "name", nullable = false, length = 80)
    private String name;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "role_permission",
            joinColumns = @JoinColumn(name = "role_id", columnDefinition = "uuid"),
            uniqueConstraints = @UniqueConstraint(name = "uk_role_perm", columnNames = {"role_id", "permission_code"})
    )
    @Column(name = "permission_code", nullable = false, length = 80)
    private Set<String> permissions = new HashSet<>();

    protected Role() {}

    private Role(String name) {
        this.name = name;
    }

    public static Role create(UUID tenantId, UUID actorId, String name, Set<String> perms) {
        if (tenantId == null) throw new IllegalArgumentException("tenantId requerido");
        if (name == null || name.trim().isBlank()) throw new IllegalArgumentException("name requerido");

        Role r = new Role(name.trim());
        r.assignTenant(tenantId);
        r.auditCreate(actorId);

        r.replacePermissions(actorId, perms);
        return r;
    }

    public UUID getId() { return super.getId(); }
    public String getName() { return name; }
    public Set<String> getPermissions() { return permissions; }

    public void rename(UUID actorId, String newName) {
        if (newName == null || newName.trim().isBlank()) throw new IllegalArgumentException("newName requerido");
        this.name = newName.trim();
        auditUpdate(actorId);
    }

    public void replacePermissions(UUID actorId, Set<String> newPerms) {
        this.permissions.clear();
        if (newPerms != null) {
            newPerms.stream()
                    .filter(Objects::nonNull)
                    .map(String::trim)
                    .filter(s -> !s.isBlank())
                    .forEach(this.permissions::add);
        }
        auditUpdate(actorId);
    }

    public void grant(UUID actorId, String perm) {
        if (perm == null || perm.trim().isBlank()) return;
        this.permissions.add(perm.trim());
        auditUpdate(actorId);
    }

    public void revoke(UUID actorId, String perm) {
        if (perm == null || perm.trim().isBlank()) return;
        this.permissions.remove(perm.trim());
        auditUpdate(actorId);
    }
}
