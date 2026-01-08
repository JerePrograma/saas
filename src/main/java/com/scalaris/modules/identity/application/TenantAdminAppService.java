package com.scalaris.modules.identity.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.scalaris.modules.identity.api.dto.TenantBootstrapDtos;
import com.scalaris.modules.identity.api.dto.TenantDtos;
import com.scalaris.modules.identity.domain.Modules;
import com.scalaris.modules.identity.domain.entity.*;
import com.scalaris.modules.identity.infrastructure.jpa.*;
import com.scalaris.shared.errors.BusinessRuleException;
import com.scalaris.shared.errors.NotFoundException;
import com.scalaris.shared.security.Permissions;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class TenantAdminAppService {

    private static final String KEY_TYPE_SLUG = "SLUG";

    private final TenantJpaRepository tenantRepo;
    private final TenantKeyJpaRepository tenantKeyRepo;
    private final TenantEntitlementJpaRepository entRepo;
    private final UserJpaRepository userRepo;
    private final RoleJpaRepository roleRepo;
    private final PasswordEncoder passwordEncoder;
    private final ObjectMapper objectMapper;

    public TenantAdminAppService(
            TenantJpaRepository tenantRepo,
            TenantKeyJpaRepository tenantKeyRepo,
            TenantEntitlementJpaRepository entRepo,
            UserJpaRepository userRepo,
            RoleJpaRepository roleRepo,
            PasswordEncoder passwordEncoder,
            ObjectMapper objectMapper
    ) {
        this.tenantRepo = tenantRepo;
        this.tenantKeyRepo = tenantKeyRepo;
        this.entRepo = entRepo;
        this.userRepo = userRepo;
        this.roleRepo = roleRepo;
        this.passwordEncoder = passwordEncoder;
        this.objectMapper = objectMapper;
    }

    // -------------------------------------------------------------------------
    // Tenant CRUD (Platform)
    // -------------------------------------------------------------------------

    @Transactional
    public TenantDtos.TenantResponse create(UUID actorId, TenantDtos.TenantCreateRequest req) {
        String name = normalizeName(req.name());

        if (tenantRepo.existsByNameIgnoreCase(name)) {
            throw new BusinessRuleException("TENANT_DUPLICATE_NAME", "Ya existe un tenant con ese nombre");
        }

        Tenant.TenantPlan plan = parsePlan(req.plan());
        String settingsJson = toJsonStringOrDefault(req.settings());

        Tenant t = Tenant.create(actorId, name, plan, settingsJson);
        Tenant saved = tenantRepo.save(t);

        // slug estable + único
        String baseSlug = slugify(name);
        String finalSlug = allocateAvailableSlug(baseSlug);
        tenantKeyRepo.save(TenantKey.create(actorId, saved.getId(), KEY_TYPE_SLUG, finalSlug));

        seedEntitlements(actorId, saved.getId(), saved.getPlan());
        return toResponse(saved);
    }

    @Transactional
    public TenantDtos.TenantResponse update(UUID actorId, UUID tenantId, TenantDtos.TenantUpdateRequest req) {
        Tenant t = tenantRepo.findById(tenantId).orElseThrow(() -> new NotFoundException("Tenant no encontrado"));

        String name = normalizeName(req.name());
        if (!name.equalsIgnoreCase(t.getName()) && tenantRepo.existsByNameIgnoreCase(name)) {
            throw new BusinessRuleException("TENANT_DUPLICATE_NAME", "Ya existe un tenant con ese nombre");
        }

        String settingsJson = toJsonStringOrDefault(req.settings());
        t.update(actorId, name, settingsJson);

        // Nota: no cambio slug automáticamente para no romper URLs.
        return toResponse(t);
    }

    @Transactional(readOnly = true)
    public TenantDtos.TenantResponse get(UUID tenantId) {
        Tenant t = tenantRepo.findById(tenantId).orElseThrow(() -> new NotFoundException("Tenant no encontrado"));
        return toResponse(t);
    }

    @Transactional(readOnly = true)
    public List<TenantDtos.TenantResponse> list() {
        return tenantRepo.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional
    public TenantDtos.TenantResponse changeStatus(UUID actorId, UUID tenantId, TenantDtos.TenantChangeStatusRequest req) {
        Tenant t = tenantRepo.findById(tenantId).orElseThrow(() -> new NotFoundException("Tenant no encontrado"));
        t.changeStatus(actorId, parseStatus(req.status()));
        return toResponse(t);
    }

    @Transactional
    public TenantDtos.TenantResponse changePlan(UUID actorId, UUID tenantId, TenantDtos.TenantChangePlanRequest req) {
        Tenant t = tenantRepo.findById(tenantId).orElseThrow(() -> new NotFoundException("Tenant no encontrado"));
        Tenant.TenantPlan plan = parsePlan(req.plan());
        t.changePlan(actorId, plan);

        seedEntitlements(actorId, tenantId, plan);
        return toResponse(t);
    }

    // -------------------------------------------------------------------------
    // Bootstrap Admin (Tenant)
    // -------------------------------------------------------------------------

    @Transactional
    public TenantBootstrapDtos.BootstrapAdminResponse bootstrapAdmin(
            UUID actorId,
            UUID tenantId,
            TenantBootstrapDtos.BootstrapAdminRequest req
    ) {
        // Validación tenant existe
        tenantRepo.findById(tenantId).orElseThrow(() -> new NotFoundException("Tenant no encontrado"));

        // Solo 1 vez
        if (userRepo.existsByTenantId(tenantId)) {
            throw new BusinessRuleException("TENANT_ALREADY_BOOTSTRAPPED", "El tenant ya tiene usuarios");
        }

        final String roleName = "ADMIN";

        // Matriz base Identity (incluye login/read)
        Set<String> perms = Set.of(
                Permissions.IDENTITY_LOGIN,
                Permissions.IDENTITY_READ,
                Permissions.IDENTITY_USERS_MANAGE,
                Permissions.IDENTITY_ROLES_MANAGE
        );

        // Defensive: canonical + validate format
        Set<String> canonicalPerms = Permissions.canonicalizeAndValidate(perms);

        Role adminRole = roleRepo.findByTenantIdAndName(tenantId, roleName)
                .orElseGet(() -> roleRepo.save(Role.create(tenantId, actorId, roleName, canonicalPerms)));

        String emailLower = TokenUtil.normalizeEmail(req.email());

        User owner = User.register(
                tenantId,
                actorId,
                emailLower,
                passwordEncoder.encode(req.password()),
                req.fullName(),
                null,
                Set.of(adminRole)
        );

        User saved = userRepo.save(owner);

        // Asegurar entitlement Identity
        entRepo.findByTenantIdAndModuleCode(tenantId, Modules.IDENTITY)
                .orElseGet(() -> entRepo.save(TenantEntitlement.of(tenantId, actorId, Modules.IDENTITY, true, "{}")));

        return new TenantBootstrapDtos.BootstrapAdminResponse(
                tenantId,
                saved.getId(),
                adminRole.getId(),
                canonicalPerms
        );
    }

    // -------------------------------------------------------------------------
    // Entitlements
    // -------------------------------------------------------------------------

    private void seedEntitlements(UUID actorId, UUID tenantId, Tenant.TenantPlan plan) {
        Map<String, Boolean> defaults = switch (plan) {
            case BASIC -> Map.of(
                    Modules.IDENTITY, true,
                    Modules.INVENTORY, false,
                    Modules.QUOTES, false
            );
            case PRO, ENTERPRISE -> Map.of(
                    Modules.IDENTITY, true,
                    Modules.INVENTORY, true,
                    Modules.QUOTES, true
            );
        };

        for (var e : defaults.entrySet()) {
            entRepo.findByTenantIdAndModuleCode(tenantId, e.getKey())
                    .orElseGet(() -> entRepo.save(
                            TenantEntitlement.of(tenantId, actorId, e.getKey(), e.getValue(), "{}")
                    ));
        }
    }

    // -------------------------------------------------------------------------
    // Mapping DTOs
    // -------------------------------------------------------------------------


    private TenantDtos.TenantResponse toResponse(Tenant t) {
        String slug = tenantKeyRepo
                .findActiveByTenantAndType(t.getId(), KEY_TYPE_SLUG)
                .map(TenantKey::getKeyValue)
                .orElse(null);

        return new TenantDtos.TenantResponse(
                t.getId(),
                t.getName(),
                t.getStatus().name(),
                t.getPlan().name(),
                slug,
                parseJsonOrEmptyObject(t.getSettingsJson())
        );
    }

    private JsonNode parseJsonOrEmptyObject(String raw) {
        try {
            if (raw == null || raw.isBlank()) return objectMapper.readTree("{}");
            return objectMapper.readTree(raw);
        } catch (Exception e) {
            // tolerante: si en DB quedó basura, no tumba el endpoint
            try { return objectMapper.readTree("{}"); }
            catch (Exception ignored) { return null; } // imposible salvo ObjectMapper roto
        }
    }

    private String toJsonStringOrDefault(JsonNode node) {
        if (node == null || node.isNull()) return "{}";
        // JsonNode.toString() devuelve JSON válido
        String s = node.toString().trim();
        return s.isBlank() ? "{}" : s;
    }

    // -------------------------------------------------------------------------
    // Slug helpers
    // -------------------------------------------------------------------------

    private String allocateAvailableSlug(String baseSlug) {
        String candidate = (baseSlug == null || baseSlug.isBlank()) ? "tenant" : baseSlug;

        // intento 1: base
        if (tenantKeyRepo.findTenantIdByKeyValueCi(TenantAdminAppService.KEY_TYPE_SLUG, candidate).isEmpty()) return candidate;

        // intento N: base-2, base-3...
        for (int i = 2; i < 10_000; i++) {
            String next = candidate + "-" + i;
            if (tenantKeyRepo.findTenantIdByKeyValueCi(TenantAdminAppService.KEY_TYPE_SLUG, next).isEmpty()) return next;
        }
        throw new BusinessRuleException("TENANT_SLUG_TAKEN", "No hay slug disponible para: " + candidate);
    }

    private String slugify(String name) {
        String s = name == null ? "" : name.trim().toLowerCase(Locale.ROOT);
        // ASCII safe: si querés acentos/ñ prolijos, normalizá con java.text.Normalizer
        s = s.replaceAll("[^a-z0-9]+", "-");
        s = s.replaceAll("^-+|-+$", "");
        return s.isBlank() ? "tenant" : s;
    }

    // -------------------------------------------------------------------------
    // Validation / parsing
    // -------------------------------------------------------------------------

    private String normalizeName(String name) {
        if (name == null) throw new BusinessRuleException("TENANT_INVALID_NAME", "Nombre requerido");
        String n = name.trim();
        if (n.isBlank()) throw new BusinessRuleException("TENANT_INVALID_NAME", "Nombre requerido");
        return n;
    }

    private Tenant.TenantStatus parseStatus(String status) {
        if (status == null) throw new BusinessRuleException("TENANT_INVALID_STATUS", "Status requerido");
        String s = status.trim().toUpperCase(Locale.ROOT);
        return switch (s) {
            case "ACTIVE" -> Tenant.TenantStatus.ACTIVE;
            case "SUSPENDED" -> Tenant.TenantStatus.SUSPENDED;
            case "CANCELED" -> Tenant.TenantStatus.CANCELED;
            default -> throw new BusinessRuleException("TENANT_INVALID_STATUS", "Status inválido: " + status);
        };
    }

    private Tenant.TenantPlan parsePlan(String plan) {
        if (plan == null) return Tenant.TenantPlan.BASIC;
        String p = plan.trim().toUpperCase(Locale.ROOT);
        return switch (p) {
            case "BASIC" -> Tenant.TenantPlan.BASIC;
            case "PRO" -> Tenant.TenantPlan.PRO;
            case "ENTERPRISE" -> Tenant.TenantPlan.ENTERPRISE;
            default -> throw new BusinessRuleException("TENANT_INVALID_PLAN", "Plan inválido: " + plan);
        };
    }
}
