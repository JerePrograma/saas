package com.scalaris.shared.tenancy;

import com.scalaris.modules.identity.infrastructure.jpa.TenantKeyJpaRepository;
import com.scalaris.shared.errors.BusinessRuleException;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class TenantResolver {
    public static final String HEADER_TENANT_ID = "X-Tenant-Id";
    public static final String HEADER_TENANT_KEY = "X-Tenant-Key";

    private final TenantKeyJpaRepository keyRepo;

    public TenantResolver(TenantKeyJpaRepository keyRepo) {
        this.keyRepo = keyRepo;
    }

    public UUID requireTenantId(UUID tenantIdHeader, String tenantKeyHeader) {
        if (tenantIdHeader != null) return tenantIdHeader;

        if (tenantKeyHeader == null || tenantKeyHeader.isBlank()) {
            throw new BusinessRuleException("TENANT_REQUIRED", "Debe enviar X-Tenant-Id o X-Tenant-Key");
        }

        return keyRepo.findTenantIdByKeyValueCi("SLUG", tenantKeyHeader.trim())
                .orElseThrow(() -> new BusinessRuleException("TENANT_KEY_INVALID", "TenantKey inválida"));
    }
}
