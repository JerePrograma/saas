// ============================================================================
// shared/sequence/SequenceService.java
// ============================================================================
package com.scalaris.shared.sequence;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class SequenceService {

    private final TenantSequenceRepository repo;

    public SequenceService(TenantSequenceRepository repo) {
        this.repo = repo;
    }

    @Transactional
    public long next(UUID tenantId, UUID actorId, SequenceKey key) {
        TenantSequence s = repo.findForUpdate(tenantId, key)
                .orElseGet(() -> repo.save(TenantSequence.start(tenantId, actorId, key, 1L)));

        return s.nextAndIncrement();
    }
}
