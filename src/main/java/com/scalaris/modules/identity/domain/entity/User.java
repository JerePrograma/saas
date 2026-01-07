// ============================================================================
// modules/identity/domain/entity/User.java
// ============================================================================
package com.scalaris.modules.identity.domain.entity;

import com.scalaris.shared.base.AuditedTenantEntity;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Entity
@Table(
        name = "app_user",
        uniqueConstraints = @UniqueConstraint(name = "uk_user_tenant_email", columnNames = {"tenant_id", "email"})
)
public class User extends AuditedTenantEntity {

    @Column(name = "email", nullable = false, length = 180)
    private String email;

    // IMPORTANTE: ahora nullable para soportar PENDING/invitación
    @Column(name = "password_hash", nullable = true, length = 200)
    private String passwordHash;

    @Column(name = "full_name", nullable = false, length = 120)
    private String fullName;

    @Column(name = "phone", length = 40)
    private String phone;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private UserStatus status = UserStatus.PENDING;

    @Column(name = "last_login_at")
    private OffsetDateTime lastLoginAt;

    // Security stamp: cambia ante roles/status/password. Invalida sesiones viejas.
    @Column(name = "security_stamp", columnDefinition = "uuid", nullable = false)
    private UUID securityStamp;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "user_role",
            joinColumns = @JoinColumn(name = "user_id", columnDefinition = "uuid"),
            inverseJoinColumns = @JoinColumn(name = "role_id", columnDefinition = "uuid"),
            uniqueConstraints = @UniqueConstraint(name = "uk_user_role", columnNames = {"user_id", "role_id"})
    )
    private Set<Role> roles = new HashSet<>();

    @Column(name = "failed_login_count", nullable = false)
    private int failedLoginCount = 0;

    @Column(name = "locked_until")
    private OffsetDateTime lockedUntil;

    @Column(name = "last_failed_login_at")
    private OffsetDateTime lastFailedLoginAt;

    protected User() {}

    private User(String emailLower, String fullName, String phone) {
        this.email = emailLower;
        this.fullName = fullName;
        this.phone = phone;
        this.securityStamp = UUID.randomUUID();
    }

    public enum UserStatus {
        PENDING,     // invitado, sin password aún
        ACTIVE,      // puede loguear
        INACTIVE,    // deshabilitado (CUS-05)
        LOCKED       // bloqueado seguridad/admin
    }

    public UUID getId() { return super.getId(); }
    public UUID getTenantId() { return super.getTenantId(); }

    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getFullName() { return fullName; }
    public String getPhone() { return phone; }
    public UserStatus getStatus() { return status; }
    public OffsetDateTime getLastLoginAt() { return lastLoginAt; }
    public UUID getSecurityStamp() { return securityStamp; }
    public Set<Role> getRoles() { return roles; }

    // -------- Factories --------

    /** Crea usuario PENDING sin password (invitación / alta sin credenciales). */
    public static User invite(UUID tenantId, UUID actorId, String emailLower, String fullName, String phone, Set<Role> roles) {
        requireBasics(tenantId, actorId, emailLower, fullName);
        User u = new User(emailLower.trim(), fullName.trim(), normalizePhone(phone));
        u.assignTenant(tenantId);
        u.auditCreate(actorId);
        u.status = UserStatus.PENDING;
        u.passwordHash = null;
        u.setRolesInternal(roles);
        return u;
    }

    /** Crea usuario ACTIVE con password. */
    public static User register(UUID tenantId, UUID actorId, String emailLower, String passwordHash, String fullName, String phone, Set<Role> roles) {
        requireBasics(tenantId, actorId, emailLower, fullName);
        if (passwordHash == null || passwordHash.isBlank()) throw new IllegalArgumentException("passwordHash requerido");

        User u = new User(emailLower.trim(), fullName.trim(), normalizePhone(phone));
        u.assignTenant(tenantId);
        u.auditCreate(actorId);
        u.status = UserStatus.ACTIVE;
        u.passwordHash = passwordHash;
        u.setRolesInternal(roles);
        return u;
    }

    // -------- Mutations --------

    public void updateProfile(UUID actorId, String fullName, String phone) {
        if (fullName != null) {
            String fn = fullName.trim();
            if (!fn.isBlank()) this.fullName = fn;
        }
        if (phone != null) {
            this.phone = normalizePhone(phone);
        }
        auditUpdate(actorId);
    }

    /** Cambio sensible: rota stamp (invalida sesiones). */
    public void setStatus(UUID actorId, UserStatus newStatus) {
        if (newStatus == null) return;

        // ACTIVE exige password
        if (newStatus == UserStatus.ACTIVE && (passwordHash == null || passwordHash.isBlank())) {
            throw new IllegalStateException("No se puede activar usuario sin password");
        }

        this.status = newStatus;
        rotateSecurityStamp();
        auditUpdate(actorId);
    }

    /** Cambio sensible: rota stamp (invalida sesiones). */
    public void setRoles(UUID actorId, Set<Role> newRoles) {
        this.roles.clear();
        if (newRoles != null) this.roles.addAll(newRoles);
        rotateSecurityStamp();
        auditUpdate(actorId);
    }

    /** Cambio sensible: rota stamp (invalida sesiones). */
    public void setPasswordHash(UUID actorId, String passwordHash) {
        if (passwordHash == null || passwordHash.isBlank()) throw new IllegalArgumentException("passwordHash requerido");
        this.passwordHash = passwordHash;
        // si venía PENDING, activar automáticamente
        if (this.status == UserStatus.PENDING) this.status = UserStatus.ACTIVE;
        rotateSecurityStamp();
        auditUpdate(actorId);
    }

    public void markLogin(OffsetDateTime now, UUID actorId) {
        this.lastLoginAt = now;
        auditUpdate(actorId);
    }

    private void rotateSecurityStamp() {
        this.securityStamp = UUID.randomUUID();
    }

    private void setRolesInternal(Set<Role> roles) {
        this.roles.clear();
        if (roles != null) this.roles.addAll(roles);
    }

    private static void requireBasics(UUID tenantId, UUID actorId, String emailLower, String fullName) {
        if (tenantId == null) throw new IllegalArgumentException("tenantId requerido");
        if (actorId == null) throw new IllegalArgumentException("actorId requerido");
        if (emailLower == null || emailLower.isBlank()) throw new IllegalArgumentException("email requerido");
        if (fullName == null || fullName.trim().isBlank()) throw new IllegalArgumentException("fullName requerido");
    }

    private static String normalizePhone(String phone) {
        if (phone == null) return null;
        String p = phone.trim();
        return p.isBlank() ? null : p;
    }

    public void changeEmail(UUID actorId, String newEmailLower) {
        if (newEmailLower == null || newEmailLower.isBlank()) {
            throw new IllegalArgumentException("email requerido");
        }
        String e = newEmailLower.trim().toLowerCase();
        if (e.length() > 180) throw new IllegalArgumentException("email demasiado largo");
        if (!e.contains("@")) throw new IllegalArgumentException("email inválido");

        if (!e.equals(this.email)) {
            this.email = e;
            rotateSecurityStamp(); // invalida sesiones
            auditUpdate(actorId);
        }
    }

    public boolean isLockedByAttempts(OffsetDateTime now) {
        return lockedUntil != null && now != null && lockedUntil.isAfter(now);
    }

    public void registerFailedLoginAttempt(OffsetDateTime now, int maxAttempts, int lockMinutes) {
        if (now == null) return;

        this.lastFailedLoginAt = now;
        this.failedLoginCount = Math.max(0, this.failedLoginCount) + 1;

        if (maxAttempts > 0 && this.failedLoginCount >= maxAttempts) {
            this.lockedUntil = now.plusMinutes(Math.max(1, lockMinutes));
            // opcional: resetear contador al bloquear
            // this.failedLoginCount = 0;
        }
    }

    public void clearFailedLogins(OffsetDateTime now) {
        this.failedLoginCount = 0;
        this.lockedUntil = null;
        this.lastFailedLoginAt = null;
    }

}
