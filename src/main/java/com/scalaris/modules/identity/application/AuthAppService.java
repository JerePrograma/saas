package com.scalaris.modules.identity.application;

import com.scalaris.modules.identity.api.dto.AuthDtos;
import com.scalaris.modules.identity.domain.IdentityErrorCodes;
import com.scalaris.modules.identity.domain.entity.*;
import com.scalaris.modules.identity.infrastructure.jpa.PasswordResetTokenJpaRepository;
import com.scalaris.modules.identity.infrastructure.jpa.TenantJpaRepository;
import com.scalaris.modules.identity.infrastructure.jpa.UserJpaRepository;
import com.scalaris.modules.identity.infrastructure.jpa.UserSessionJpaRepository;
import com.scalaris.shared.errors.BusinessRuleException;
import com.scalaris.shared.errors.NotFoundException;
import com.scalaris.shared.security.Permissions;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AuthAppService {

    private final UserJpaRepository userRepo;
    private final UserSessionJpaRepository sessionRepo;
    private final PasswordResetTokenJpaRepository prtRepo;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher events;
    private final TenantJpaRepository tenantRepo;

    public AuthAppService(UserJpaRepository userRepo,
                          UserSessionJpaRepository sessionRepo,
                          PasswordResetTokenJpaRepository prtRepo,
                          PasswordEncoder passwordEncoder,
                          ApplicationEventPublisher events, TenantJpaRepository tenantRepo) {
        this.userRepo = userRepo;
        this.sessionRepo = sessionRepo;
        this.prtRepo = prtRepo;
        this.passwordEncoder = passwordEncoder;
        this.events = events;
        this.tenantRepo = tenantRepo;
    }

    @Transactional
    public AuthDtos.LoginResponse login(UUID tenantId, AuthDtos.LoginRequest req, String ip, String userAgent) {
        if (tenantId == null || req == null) {
            throw new BusinessRuleException(IdentityErrorCodes.AUTH_INVALID_CREDENTIALS, "Credenciales inválidas");
        }

        // Tenant debe existir y estar ACTIVE (CUS-01)
        Tenant t = tenantRepo.findById(tenantId).orElse(null);
        if (t == null || t.getStatus() != Tenant.TenantStatus.ACTIVE) {
            // podés colapsar a invalid creds para no filtrar info
            throw new BusinessRuleException("AUTH_TENANT_INACTIVE", "Tenant inactivo");
        }

        String emailLower = TokenUtil.normalizeEmail(req.email());

        User user = userRepo.findByTenantAndEmailLower(tenantId, emailLower)
                .orElseThrow(() -> new BusinessRuleException(
                        IdentityErrorCodes.AUTH_INVALID_CREDENTIALS, "Credenciales inválidas"
                ));

        OffsetDateTime now = OffsetDateTime.now();

        // Lockout por intentos fallidos (CUS-01) — requiere campos persistidos (ver más abajo)
        if (user.isLockedByAttempts(now)) {
            throw new BusinessRuleException(IdentityErrorCodes.AUTH_USER_BLOCKED, "Usuario bloqueado");
        }

        // Gate por estado (CUS-01)
        if (user.getStatus() == User.UserStatus.LOCKED) {
            throw new BusinessRuleException(IdentityErrorCodes.AUTH_USER_BLOCKED, "Usuario bloqueado");
        }
        if (user.getStatus() == User.UserStatus.INACTIVE) {
            throw new BusinessRuleException(IdentityErrorCodes.AUTH_USER_INACTIVE, "Usuario inactivo");
        }
        if (user.getStatus() == User.UserStatus.PENDING) {
            throw new BusinessRuleException(IdentityErrorCodes.AUTH_USER_PENDING, "Usuario pendiente de activación");
        }

        // Validación credenciales
        if (user.getPasswordHash() == null || user.getPasswordHash().isBlank()
                || req.password() == null || req.password().isBlank()
                || !passwordEncoder.matches(req.password(), user.getPasswordHash())) {

            // Registrar fallo (lockout) — requiere persistencia
            // Lockout (configurable)
            int maxFailedAttempts = 5;
            int lockMinutes = 15;
            user.registerFailedLoginAttempt(now, maxFailedAttempts, lockMinutes);
            userRepo.save(user);

            throw new BusinessRuleException(IdentityErrorCodes.AUTH_INVALID_CREDENTIALS, "Credenciales inválidas");
        }

        // Éxito => limpiar contadores lockout
        user.clearFailedLogins(now);
        user.markLogin(now, user.getId()); // actor=self
        userRepo.save(user);

        boolean remember = Boolean.TRUE.equals(req.rememberMe());
        // Defaults razonables (configurables)
        int sessionHoursDefault = 8;
        int rememberDaysDefault = 30;
        OffsetDateTime expires = remember
                ? now.plusDays(rememberDaysDefault)
                : now.plusHours(sessionHoursDefault);

        String rawToken = TokenUtil.newOpaqueToken();
        String tokenHash = TokenUtil.sha256Hex(rawToken);

        UserSession session = UserSession.open(
                tenantId,
                user.getId(),
                user.getId(),
                user.getSecurityStamp(),
                tokenHash,
                now,
                expires,
                ip,
                userAgent
        );
        sessionRepo.save(session);

        User authUser = userRepo.fetchAuthGraph(tenantId, user.getId())
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

        Set<String> roles = authUser.getRoles().stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        // Canonical perms + validate (evita basura)
        Set<String> perms = authUser.getRoles().stream()
                .flatMap(r -> r.getPermissions().stream())
                .map(Permissions::canonicalize)
                .filter(Permissions::isCanonical)
                .collect(Collectors.toUnmodifiableSet());

        // CUS-01: contexto de permisos no puede estar vacío
        if (perms.isEmpty()) {
            throw new BusinessRuleException("AUTH_EMPTY_PERMISSIONS", "El contexto de permisos no puede estar vacío");
        }

        // Matriz: Login => identity.login
        if (!perms.contains(Permissions.IDENTITY_LOGIN)) {
            throw new BusinessRuleException("AUTH_LOGIN_NOT_ALLOWED", "Login no permitido");
        }

        // Evento login (CUS-01)
        events.publishEvent(new LoginSucceededEvent(tenantId, authUser.getId(), ip, userAgent, now));

        return new AuthDtos.LoginResponse(
                tenantId,
                authUser.getId(),
                authUser.getFullName(),
                authUser.getEmail(),
                rawToken,
                roles,
                perms
        );
    }

    public record LoginSucceededEvent(UUID tenantId, UUID userId, String ip, String userAgent, OffsetDateTime at) {}

    @Transactional
    public void logout(UUID tenantId, UUID sessionUserId, String rawToken) {
        if (tenantId == null || sessionUserId == null) return;
        if (rawToken == null || rawToken.isBlank()) return;

        String hash = TokenUtil.sha256Hex(rawToken);
        UserSession s = sessionRepo.findBySessionTokenHash(hash)
                .orElseThrow(() -> new NotFoundException("Sesión no encontrada"));

        if (!tenantId.equals(s.getTenantId()) || !sessionUserId.equals(s.getUserId())) {
            // no revelar: “no encontrada”
            throw new BusinessRuleException(IdentityErrorCodes.AUTH_SESSION_NOT_FOUND, "Sesión no encontrada");
        }

        if (s.getRevokedAt() != null) return; // idempotente
        s.revoke(OffsetDateTime.now(), sessionUserId);
        sessionRepo.save(s);
    }

    @Transactional(readOnly = true)
    public AuthDtos.MeResponse me(UUID tenantId, UUID userId) {
        User u = userRepo.fetchAuthGraph(tenantId, userId)
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

        Set<String> roles = u.getRoles().stream().map(Role::getName).collect(Collectors.toSet());
        Set<String> perms = u.getRoles().stream().flatMap(r -> r.getPermissions().stream()).collect(Collectors.toSet());

        return new AuthDtos.MeResponse(
                tenantId, u.getId(), u.getFullName(), u.getEmail(), u.getStatus().name(),
                roles, perms
        );
    }

    @Transactional
    public void requestPasswordReset(UUID tenantId, AuthDtos.PasswordResetRequest req) {
        if (tenantId == null || req == null) return;

        String emailLower = TokenUtil.normalizeEmail(req.email());

        // No revelar existencia: siempre OK.
        userRepo.findByTenantAndEmailLower(tenantId, emailLower).ifPresent(user -> {
            if (user.getStatus() == User.UserStatus.INACTIVE) return;

            OffsetDateTime now = OffsetDateTime.now();

            // opcional recomendable: invalidar tokens previos del mismo propósito
            prtRepo.invalidateAllUsableForUser(tenantId, user.getId(), PasswordResetToken.Purpose.RESET, now);

            String raw = TokenUtil.newOpaqueToken();
            String hash = TokenUtil.sha256Hex(raw);

            PasswordResetToken t = PasswordResetToken.issue(
                    tenantId,
                    user.getId(), // actorId=self
                    user.getId(),
                    PasswordResetToken.Purpose.RESET,
                    hash,
                    now.plusMinutes(30)
            );
            prtRepo.save(t);

            events.publishEvent(new PasswordResetRequestedEvent(
                    tenantId, user.getId(), user.getEmail(), raw
            ));
        });
    }

    @Transactional
    public void confirmPasswordReset(UUID tenantId, AuthDtos.PasswordResetConfirm req) {
        if (tenantId == null || req == null) {
            throw new BusinessRuleException(IdentityErrorCodes.AUTH_RESET_TOKEN_INVALID, "Token inválido o expirado");
        }
        if (req.token() == null || req.token().isBlank()) {
            throw new BusinessRuleException(IdentityErrorCodes.AUTH_RESET_TOKEN_INVALID, "Token inválido o expirado");
        }
        if (req.newPassword() == null || req.newPassword().isBlank()) {
            throw new BusinessRuleException(IdentityErrorCodes.AUTH_RESET_TOKEN_INVALID, "Token inválido o expirado");
        }

        OffsetDateTime now = OffsetDateTime.now();
        String hash = TokenUtil.sha256Hex(req.token());

        PasswordResetToken prt = prtRepo.findUsable(tenantId, hash, PasswordResetToken.Purpose.RESET, now)
                .orElseThrow(() -> new BusinessRuleException(
                        IdentityErrorCodes.AUTH_RESET_TOKEN_INVALID, "Token inválido o expirado"
                ));

        User u = userRepo.findById(prt.getUserId())
                .orElseThrow(() -> new NotFoundException("Usuario no encontrado"));

        if (!tenantId.equals(u.getTenantId())) {
            throw new BusinessRuleException(IdentityErrorCodes.AUTH_RESET_TOKEN_INVALID, "Token inválido o expirado");
        }

        // setPasswordHash rota securityStamp => mata sesiones viejas por el gate del filter
        u.setPasswordHash(u.getId(), passwordEncoder.encode(req.newPassword()));
        prt.markUsed(now, u.getId());

        // además: revocar explícitamente sesiones activas (defensa en profundidad)
        sessionRepo.revokeAllActiveForUser(tenantId, u.getId(), now, u.getId());
    }

    public record PasswordResetRequestedEvent(UUID tenantId, UUID userId, String email, String rawToken) {}
}
