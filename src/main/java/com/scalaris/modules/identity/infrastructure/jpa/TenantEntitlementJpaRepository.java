package com.scalaris.modules.identity.infrastructure.jpa;

import com.scalaris.modules.identity.domain.entity.TenantEntitlement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface TenantEntitlementJpaRepository extends JpaRepository<TenantEntitlement, UUID> {
    Optional<TenantEntitlement> findByTenantIdAndModuleCode(UUID tenantId, String moduleCode);
    List<TenantEntitlement> findAllByTenantId(UUID tenantId);
}
