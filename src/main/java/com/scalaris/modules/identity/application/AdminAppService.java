// ============================================================================
// modules/identity/application/AdminAppService.java
// ============================================================================
//
// Convención aplicada:
// - Permisos: CANÓNICOS en lower (Permissions.*).
// - Entrada (DTO/DB): se canonicaliza (trim + lower) antes de comparar/persistir.
// - Validación de formato: lower.dot (sin wildcards tipo identity:*).
// - Subset checks y lockout guard trabajan siempre con permisos canonicalizados.
// ============================================================================
package com.scalaris.modules.identity.application;

import com.scalaris.modules.identity.api.dto.AdminDtos;
import com.scalaris.modules.identity.domain.entity.Role;
import com.scalaris.modules.identity.domain.entity.User;
import com.scalaris.modules.identity.domain.entity.UserSession;
import com.scalaris.modules.identity.infrastructure.jpa.RoleJpaRepository;
import com.scalaris.modules.identity.infrastructure.jpa.UserJpaRepository;
import com.scalaris.modules.identity.infrastructure.jpa.UserSessionJpaRepository;
import com.scalaris.shared.errors.BusinessRuleException;
import com.scalaris.shared.errors.NotFoundException;
import com.scalaris.shared.security.Permissions;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AdminAppService {

    private final UserJpaRepository userRepo;
    private final RoleJpaRepository roleRepo;
    private final UserSessionJpaRepository sessionRepo;
    private final PasswordEncoder passwordEncoder;

    public AdminAppService(UserJpaRepository userRepo,
                           RoleJpaRepository roleRepo,
                           UserSessionJpaRepository sessionRepo,
                           PasswordEncoder passwordEncoder) {
        this.userRepo = userRepo;
        this.roleRepo = roleRepo;
        this.sessionRepo = sessionRepo;
        this.passwordEncoder = passwordEncoder;
    }

    // -------------------- ROLES --------------------

    @Transactional
    public AdminDtos.RoleResponse createRole(UUID tenantId, UUID actorId, AdminDtos.RoleUpsertRequest req) {
        ActorCtx actor = loadActor(tenantId, actorId);
        require(actor.perms, Permissions.IDENTITY_ROLES_MANAGE);

        String name = normalizeRoleName(req.name());
        Set<String> perms = normalizePerms(req.permissions()); // canonical lower + format validate

        requireSubset(
                "ROLE_PERM_OUT_OF_BOUNDS",
                "No podés asignar permisos que no tenés",
                perms,
                actor.perms
        );

        if (roleRepo.existsByTenantIdAndName(tenantId, name)) {
            throw new BusinessRuleException("ROLE_DUPLICATE", "Ya existe un rol con ese nombre");
        }

        Role r = Role.create(tenantId, actorId, name, perms);
        Role saved = roleRepo.save(r);

        return new AdminDtos.RoleResponse(saved.getId(), saved.getName(), saved.getPermissions());
    }

    @Transactional
    public AdminDtos.RoleResponse updateRole(UUID tenantId, UUID actorId, UUID roleId, AdminDtos.RoleUpsertRequest req) {
        ActorCtx actor = loadActor(tenantId, actorId);
        require(actor.perms, Permissions.IDENTITY_ROLES_MANAGE);

        Role r = roleRepo.findById(roleId).orElseThrow(() -> new NotFoundException("Rol no encontrado"));
        assertTenant(tenantId, r.getTenantId(), "Rol no encontrado");

        String newName = normalizeRoleName(req.name());
        if (!newName.equals(r.getName()) && roleRepo.existsByTenantIdAndName(tenantId, newName)) {
            throw new BusinessRuleException("ROLE_DUPLICATE", "Ya existe un rol con ese nombre");
        }

        Set<String> newPerms = normalizePerms(req.permissions());
        requireSubset(
                "ROLE_PERM_OUT_OF_BOUNDS",
                "No podés asignar permisos que no tenés",
                newPerms,
                actor.perms
        );

        // Guard fuerte: si el actor tiene este rol, no permitas sacarse permisos críticos
        preventSelfLockoutByRoleEdit(actor, r, newPerms);

        boolean permsChanged = !Objects.equals(r.getPermissions(), newPerms);

        if (!newName.equals(r.getName())) r.rename(actorId, newName);
        if (permsChanged) r.replacePermissions(actorId, newPerms);

        if (permsChanged) {
            revokeSessionsForUsersWithRole(tenantId, roleId, actorId);
        }

        // Regla fuerte: no permitir que el tenant quede sin admins activos
        ensureAtLeastOneActiveUserHasPerm(tenantId, Permissions.IDENTITY_USERS_MANAGE);

        return new AdminDtos.RoleResponse(r.getId(), r.getName(), r.getPermissions());
    }

    @Transactional(readOnly = true)
    public List<AdminDtos.RoleResponse> listRoles(UUID tenantId) {
        return roleRepo.findAllByTenantId(tenantId).stream()
                .map(x -> new AdminDtos.RoleResponse(x.getId(), x.getName(), x.getPermissions()))
                .toList();
    }

    // -------------------- USERS --------------------

    @Transactional
    public AdminDtos.UserResponse createUser(UUID tenantId, UUID actorId, AdminDtos.UserCreateRequest req) {
        ActorCtx actor = loadActor(tenantId, actorId);
        require(actor.perms, Permissions.IDENTITY_USERS_MANAGE);

        String emailLower = TokenUtil.normalizeEmail(req.email());

        if (userRepo.existsByTenantIdAndEmailIgnoreCase(tenantId, emailLower)) {
            throw new BusinessRuleException("EMAIL_DUPLICATE", "El email ya está registrado.");
        }

        var roles = resolveRolesStrict(tenantId, req.roleIds());
        requireAtLeastOneRole(roles);

        // CUS-04 regla aplicada también acá: no crear usuarios con más permisos que el actor
        Set<String> targetPerms = unionPerms(roles);
        requireSubset("ROLE_ASSIGN_OUT_OF_BOUNDS",
                "No podés asignar un rol con más permisos que los tuyos",
                targetPerms, actor.perms);

        User u;
        if (req.password() == null || req.password().isBlank()) {
            u = User.invite(tenantId, actorId, emailLower, req.fullName(), req.phone(), roles);
        } else {
            u = User.register(
                    tenantId, actorId, emailLower,
                    passwordEncoder.encode(req.password()),
                    req.fullName(), req.phone(), roles
            );
        }

        User saved = userRepo.save(u);
        return toUserResponse(saved);
    }

    @Transactional
    public AdminDtos.UserResponse updateUser(UUID tenantId, UUID actorId, UUID userId, AdminDtos.UserUpdateRequest req) {
        ActorCtx actor = loadActor(tenantId, actorId);
        require(actor.perms, Permissions.IDENTITY_USERS_MANAGE);

        User u = userRepo.fetchAuthGraph(tenantId, userId)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

        // “no se puede editar un usuario eliminado”
        if (!u.isActive()) {
            throw new BusinessRuleException("USER_DELETED", "No se puede editar un usuario eliminado");
        }

        boolean sensitiveChange = false;

        // Email (si viene)
        if (req.email() != null && !req.email().isBlank()) {
            String emailLower = TokenUtil.normalizeEmail(req.email());

            // Validación duplicado por tenant
            if (userRepo.existsByTenantIdAndEmailIgnoreCaseAndIdNot(tenantId, emailLower, u.getId())) {
                throw new BusinessRuleException("EMAIL_DUPLICATE", "El email ya está registrado.");
            }

            u.changeEmail(actorId, emailLower);
            sensitiveChange = true;
        }

        if (req.fullName() != null || req.phone() != null) {
            u.updateProfile(actorId, req.fullName(), req.phone());
            // perfil no necesariamente invalida sesión; si querés más estricto, poné true
        }

        if (sensitiveChange) {
            revokeAllSessions(tenantId, u.getId(), actorId);
        }

        return toUserResponse(u);
    }

    @Transactional
    public void resetUserPassword(UUID tenantId, UUID actorId, UUID userId, String newPassword) {
        ActorCtx actor = loadActor(tenantId, actorId);
        require(actor.perms, Permissions.IDENTITY_USERS_MANAGE);

        if (newPassword == null || newPassword.isBlank()) {
            throw new BusinessRuleException("INVALID_PASSWORD", "Password inválida");
        }

        User u = userRepo.findById(userId).orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
        assertTenant(tenantId, u.getTenantId(), "Usuario no encontrado");

        u.setPasswordHash(actorId, passwordEncoder.encode(newPassword));
        revokeAllSessions(tenantId, u.getId(), actorId);
    }

    @Transactional
    public void blockUser(UUID tenantId, UUID actorId, UUID userId) {
        ActorCtx actor = loadActor(tenantId, actorId);
        require(actor.perms, Permissions.IDENTITY_USERS_MANAGE);

        if (actorId.equals(userId)) {
            throw new BusinessRuleException("SELF_LOCKOUT", "No podés bloquearte a vos mismo");
        }

        User u = userRepo.fetchAuthGraph(tenantId, userId)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

        if (userHasPerm(u, Permissions.IDENTITY_USERS_MANAGE)) {
            ensureNotLastAdmin(tenantId);
        }

        u.setStatus(actorId, User.UserStatus.LOCKED);
        revokeAllSessions(tenantId, u.getId(), actorId);
    }

    @Transactional
    public void activateUser(UUID tenantId, UUID actorId, UUID userId) {
        ActorCtx actor = loadActor(tenantId, actorId);
        require(actor.perms, Permissions.IDENTITY_USERS_MANAGE);

        User u = userRepo.findById(userId).orElseThrow(() -> new NotFoundException("Usuario no encontrado"));
        assertTenant(tenantId, u.getTenantId(), "Usuario no encontrado");

        u.setStatus(actorId, User.UserStatus.ACTIVE);
        revokeAllSessions(tenantId, u.getId(), actorId);
    }

    // -------------------- SESSION MANAGEMENT --------------------

    private void revokeAllSessions(UUID tenantId, UUID targetUserId, UUID actorId) {
        OffsetDateTime now = OffsetDateTime.now();
        List<UserSession> sessions = sessionRepo.findAllByTenantIdAndUserIdAndRevokedAtIsNull(tenantId, targetUserId);
        if (sessions.isEmpty()) return;

        for (UserSession s : sessions) {
            s.revoke(now, actorId);
        }
        sessionRepo.saveAll(sessions);
    }

    private void revokeSessionsForUsersWithRole(UUID tenantId, UUID roleId, UUID actorId) {
        List<UUID> userIds = roleRepo.findUserIdsByTenantIdAndRoleId(tenantId, roleId);
        if (userIds.isEmpty()) return;

        OffsetDateTime now = OffsetDateTime.now();
        List<UserSession> sessions = sessionRepo.findAllActiveByTenantIdAndUserIds(tenantId, userIds);
        if (sessions.isEmpty()) return;

        for (UserSession s : sessions) {
            s.revoke(now, actorId);
        }
        sessionRepo.saveAll(sessions);
    }

    // -------------------- HELPERS --------------------

    private ActorCtx loadActor(UUID tenantId, UUID actorId) {
        User actor = userRepo.fetchAuthGraph(tenantId, actorId)
                .orElseThrow(() -> new NotFoundException("Actor no encontrado"));

        if (actor.getStatus() != User.UserStatus.ACTIVE) {
            throw new BusinessRuleException("FORBIDDEN", "Actor inactivo/bloqueado");
        }

        Set<String> perms = unionPerms(actor.getRoles());
        return new ActorCtx(actor, perms);
    }

    private void require(Set<String> perms, String required) {
        String req = Permissions.canonicalize(required);
        if (req == null) throw new IllegalArgumentException("required perm null/blank");

        if (perms == null || !perms.contains(req)) {
            throw new BusinessRuleException("FORBIDDEN", "Falta permiso: " + req);
        }
    }

    private void requireSubset(String code, String msg, Set<String> target, Set<String> actor) {
        if (target == null || target.isEmpty()) return;
        Set<String> actorSet = (actor == null) ? Set.of() : actor;
        for (String p : target) {
            String perm = Permissions.canonicalize(p);
            if (perm == null) continue;
            if (!Permissions.isCanonical(perm)) continue;
            if (!actorSet.contains(perm)) {
                throw new BusinessRuleException(code, msg + " (" + perm + ")");
            }
        }
    }

    private void preventSelfLockoutByRoleEdit(ActorCtx actor, Role role, Set<String> newPerms) {
        boolean actorHasThisRole = actor.actor.getRoles().stream().anyMatch(r -> r.getId().equals(role.getId()));
        if (!actorHasThisRole) return;

        preventSelfLoss(
                actor, role, newPerms,
                Permissions.IDENTITY_ROLES_MANAGE,
                "SELF_LOCKOUT",
                "No podés sacarte el permiso de administrar roles"
        );
        preventSelfLoss(
                actor, role, newPerms,
                Permissions.IDENTITY_USERS_MANAGE,
                "SELF_LOCKOUT",
                "No podés sacarte el permiso de administrar usuarios"
        );
    }

    private void preventSelfLoss(ActorCtx actor, Role role, Set<String> newPerms, String perm, String code, String msg) {
        String p = Permissions.canonicalize(perm);
        if (p == null) return;

        boolean roleHad = role.getPermissions().contains(p);
        boolean roleWillHave = newPerms.contains(p);
        if (!roleHad || roleWillHave) return;

        boolean hasElsewhere = actor.actor.getRoles().stream()
                .filter(r -> !r.getId().equals(role.getId()))
                .anyMatch(r -> r.getPermissions().contains(p));

        if (!hasElsewhere) throw new BusinessRuleException(code, msg);
    }

    private Set<Role> resolveRolesStrict(UUID tenantId, Set<UUID> roleIds) {
        if (roleIds == null || roleIds.isEmpty()) return Set.of();

        List<Role> roles = roleRepo.findAllById(roleIds);
        if (roles.size() != roleIds.size()) {
            throw new BusinessRuleException("ROLE_NOT_FOUND", "Uno o más roles no existen");
        }

        for (Role r : roles) {
            assertTenant(tenantId, r.getTenantId(), "Rol inválido");
        }
        return new HashSet<>(roles);
    }

    private void requireAtLeastOneRole(Set<Role> roles) {
        if (roles == null || roles.isEmpty()) {
            throw new BusinessRuleException("USER_ROLE_REQUIRED", "No se puede dejar un usuario sin rol");
        }
    }

    private Set<String> unionPerms(Set<Role> roles) {
        if (roles == null || roles.isEmpty()) return Set.of();

        Set<String> out = new HashSet<>();
        for (Role r : roles) {
            if (r.getPermissions() == null) continue;
            for (String p : r.getPermissions()) {
                String c = Permissions.canonicalize(p);
                if (c == null) continue;
                if (!Permissions.isCanonical(c)) continue;
                out.add(c);
            }
        }
        return Set.copyOf(out);
    }

    private boolean userHasPerm(User u, String perm) {
        String p = Permissions.canonicalize(perm);
        if (p == null) return false;
        return unionPerms(u.getRoles()).contains(p);
    }

    private void ensureNotLastAdmin(UUID tenantId) {
        long admins = userRepo.countActiveUsersWithPermission(tenantId, Permissions.IDENTITY_USERS_MANAGE);
        if (admins <= 1) {
            throw new BusinessRuleException("LAST_ADMIN", "No se puede desactivar al último administrador del tenant");
        }
    }

    private void ensureAtLeastOneActiveUserHasPerm(UUID tenantId, String perm) {
        String p = Permissions.canonicalize(perm);
        long c = userRepo.countActiveUsersWithPermission(tenantId, p);
        if (c <= 0) {
            throw new BusinessRuleException("LAST_ADMIN", "El tenant no puede quedar sin administradores");
        }
    }

    private AdminDtos.UserResponse toUserResponse(User u) {
        Set<AdminDtos.RoleLite> roles = u.getRoles().stream()
                .map(r -> new AdminDtos.RoleLite(r.getId(), r.getName()))
                .collect(Collectors.toSet());

        return new AdminDtos.UserResponse(
                u.getId(),
                u.getEmail(),
                u.getFullName(),
                u.getPhone(),
                u.getStatus().name(),
                roles
        );
    }

    private void assertTenant(UUID expectedTenant, UUID actualTenant, String msg) {
        if (expectedTenant == null || actualTenant == null || !expectedTenant.equals(actualTenant)) {
            throw new NotFoundException(msg);
        }
    }

    private User.UserStatus parseStatus(String status) {
        if (status == null) throw new BusinessRuleException("INVALID_STATUS", "Status requerido");
        String s = status.trim().toUpperCase(Locale.ROOT);

        return switch (s) {
            case "ACTIVE" -> User.UserStatus.ACTIVE;
            case "INACTIVE" -> User.UserStatus.INACTIVE;
            case "LOCKED", "BLOCKED" -> User.UserStatus.LOCKED;
            case "PENDING" -> User.UserStatus.PENDING;
            default -> throw new BusinessRuleException("INVALID_STATUS", "Status inválido: " + status);
        };
    }

    private String normalizeRoleName(String name) {
        if (name == null) throw new BusinessRuleException("INVALID_ROLE_NAME", "Nombre de rol requerido");
        String n = name.trim().toUpperCase(Locale.ROOT);
        if (n.isBlank()) throw new BusinessRuleException("INVALID_ROLE_NAME", "Nombre de rol requerido");
        if (n.length() > 80) throw new BusinessRuleException("INVALID_ROLE_NAME", "Nombre de rol demasiado largo");
        return n;
    }

    private Set<String> normalizePerms(Set<String> perms) {
        // canonical lower + formato. No valida “catálogo” para permitir módulos futuros.
        return Permissions.canonicalizeAndValidate(perms);
    }

    private record ActorCtx(User actor, Set<String> perms) {
    }

    @Transactional
    public void deactivateUser(UUID tenantId, UUID actorId, UUID targetUserId) {
        ActorCtx actor = loadActor(tenantId, actorId);
        require(actor.perms, Permissions.IDENTITY_USERS_MANAGE);

        if (actorId.equals(targetUserId)) {
            throw new BusinessRuleException("SELF_LOCKOUT", "El usuario no puede desactivarse a sí mismo.");
        }

        User u = userRepo.fetchAuthGraph(tenantId, targetUserId)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

        if (!u.isActive()) {
            throw new BusinessRuleException("USER_DELETED", "Usuario eliminado");
        }

        if (u.getStatus() == User.UserStatus.INACTIVE) {
            throw new BusinessRuleException("USER_ALREADY_INACTIVE", "El usuario ya está inactivo.");
        }

        // Precondición CUS-05: “Usuario activo”
        if (u.getStatus() != User.UserStatus.ACTIVE) {
            throw new BusinessRuleException("USER_NOT_ACTIVE", "Solo se puede desactivar un usuario activo.");
        }

        // “No se puede desactivar al último admin”
        if (userHasPerm(u, Permissions.IDENTITY_USERS_MANAGE)) {
            ensureNotLastAdmin(tenantId);
        }

        u.setStatus(actorId, User.UserStatus.INACTIVE);
        revokeAllSessions(tenantId, u.getId(), actorId);
    }

    @Transactional
    public AdminDtos.UserResponse assignRoles(UUID tenantId, UUID actorId, UUID targetUserId, AdminDtos.AssignRolesRequest req) {
        ActorCtx actor = loadActor(tenantId, actorId);
        require(actor.perms, Permissions.IDENTITY_ROLES_MANAGE);

        User u = userRepo.fetchAuthGraph(tenantId, targetUserId)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

        if (!u.isActive()) {
            throw new BusinessRuleException("USER_DELETED", "Usuario eliminado");
        }

        // Precondición: Usuario activo
        if (u.getStatus() != User.UserStatus.ACTIVE) {
            throw new BusinessRuleException("USER_NOT_ACTIVE", "El usuario debe estar activo.");
        }

        var newRoles = resolveRolesStrict(tenantId, req.roleIds());
        requireAtLeastOneRole(newRoles);

        // Rol debe estar activo (si usás active=false como soft-delete)
        for (Role r : newRoles) {
            if (!r.isActive()) throw new BusinessRuleException("ROLE_INACTIVE", "El rol debe estar activo.");
        }

        Set<String> targetPerms = unionPerms(newRoles);

        // Regla: no asignar rol superior (sin role.level => subset por permisos)
        requireSubset("ROLE_ASSIGN_OUT_OF_BOUNDS",
                "No podés asignar un rol con más permisos que los tuyos",
                targetPerms, actor.perms);

        boolean willLoseManage = userHasPerm(u, Permissions.IDENTITY_USERS_MANAGE)
                && !targetPerms.contains(Permissions.IDENTITY_USERS_MANAGE);

        if (willLoseManage) {
            ensureNotLastAdmin(tenantId);
        }

        u.setRoles(actorId, newRoles);
        revokeAllSessions(tenantId, u.getId(), actorId);

        return toUserResponse(u);
    }

    @Transactional(readOnly = true)
    public List<AdminDtos.UserResponse> listUsers(UUID tenantId, UUID actorId) {
        ActorCtx actor = loadActor(tenantId, actorId);
        require(actor.perms, Permissions.IDENTITY_READ);

        return userRepo.fetchAllAuthGraphsByTenantId(tenantId).stream()
                .map(this::toUserResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public AdminDtos.UserResponse getUser(UUID tenantId, UUID actorId, UUID userId) {
        ActorCtx actor = loadActor(tenantId, actorId);
        require(actor.perms, Permissions.IDENTITY_READ);

        User u = userRepo.fetchAuthGraph(tenantId, userId)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

        return toUserResponse(u);
    }

}
