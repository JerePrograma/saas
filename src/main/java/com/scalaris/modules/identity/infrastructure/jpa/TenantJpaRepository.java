// ============================================================================
// modules/identity/infrastructure/jpa/TenantJpaRepository.java
// ============================================================================
package com.scalaris.modules.identity.infrastructure.jpa;

import com.scalaris.modules.identity.domain.entity.Tenant;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface TenantJpaRepository extends JpaRepository<Tenant, UUID> {

    @Query("select t from Tenant t where lower(t.name) = lower(:name)")
    Optional<Tenant> findByNameIgnoreCase(@Param("name") String name);

    @Query("select (count(t) > 0) from Tenant t where lower(t.name) = lower(:name)")
    boolean existsByNameIgnoreCase(@Param("name") String name);
}
