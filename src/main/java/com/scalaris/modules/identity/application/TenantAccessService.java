package com.scalaris.modules.identity.application;

import com.scalaris.modules.identity.domain.entity.Tenant;
import com.scalaris.modules.identity.infrastructure.jpa.TenantEntitlementJpaRepository;
import com.scalaris.modules.identity.infrastructure.jpa.TenantJpaRepository;
import com.scalaris.shared.errors.BusinessRuleException;
import com.scalaris.shared.errors.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class TenantAccessService {

    private final TenantJpaRepository tenantRepo;
    private final TenantEntitlementJpaRepository entRepo;

    public TenantAccessService(TenantJpaRepository tenantRepo, TenantEntitlementJpaRepository entRepo) {
        this.tenantRepo = tenantRepo;
        this.entRepo = entRepo;
    }

    @Transactional(readOnly = true)
    public Tenant requireActiveTenant(UUID tenantId) {
        Tenant t = tenantRepo.findById(tenantId).orElseThrow(() -> new NotFoundException("Tenant no encontrado"));
        if (t.getStatus() != Tenant.TenantStatus.ACTIVE) {
            throw new BusinessRuleException("TENANT_NOT_ACTIVE", "Tenant no activo: " + t.getStatus());
        }
        return t;
    }

    @Transactional(readOnly = true)
    public void requireModule(UUID tenantId, String moduleCode) {
        requireActiveTenant(tenantId);

        boolean enabled = entRepo.findByTenantIdAndModuleCode(tenantId, moduleCode)
                .map(e -> e.isEnabled())
                .orElse(false);

        if (!enabled) {
            throw new BusinessRuleException("MODULE_NOT_ENABLED", "Módulo no habilitado: " + moduleCode);
        }
    }
}
