package com.scalaris.modules.identity.infrastructure.jpa;

import com.scalaris.modules.identity.domain.entity.PasswordResetToken;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface PasswordResetTokenJpaRepository extends JpaRepository<PasswordResetToken, UUID> {

    @Query("""
        select t
          from PasswordResetToken t
         where t.tenantId = :tenantId
           and t.tokenHash = :hash
           and t.purpose = :purpose
           and t.usedAt is null
           and t.expiresAt > :now
        """)
    Optional<PasswordResetToken> findUsable(@Param("tenantId") UUID tenantId,
                                            @Param("hash") String hash,
                                            @Param("purpose") PasswordResetToken.Purpose purpose,
                                            @Param("now") OffsetDateTime now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update PasswordResetToken t
           set t.usedAt = :now
         where t.tenantId = :tenantId
           and t.userId = :userId
           and t.purpose = :purpose
           and t.usedAt is null
           and t.expiresAt > :now
        """)
    void invalidateAllUsableForUser(@Param("tenantId") UUID tenantId,
                                   @Param("userId") UUID userId,
                                   @Param("purpose") PasswordResetToken.Purpose purpose,
                                   @Param("now") OffsetDateTime now);
}
