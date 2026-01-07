// ============================================================================
// shared/sequence/TenantSequenceRepository.java
// ============================================================================
package com.scalaris.shared.sequence;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface TenantSequenceRepository extends JpaRepository<TenantSequence, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from TenantSequence s where s.tenantId = :tenantId and s.key = :key")
    Optional<TenantSequence> findForUpdate(@Param("tenantId") UUID tenantId, @Param("key") SequenceKey key);

    Optional<TenantSequence> findByTenantIdAndKey(UUID tenantId, SequenceKey key);
}
