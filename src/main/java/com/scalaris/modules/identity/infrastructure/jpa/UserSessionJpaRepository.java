package com.scalaris.modules.identity.infrastructure.jpa;

import com.scalaris.modules.identity.domain.entity.UserSession;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserSessionJpaRepository extends JpaRepository<UserSession, UUID> {

    @Query("""
           select s from UserSession s
           where s.tenantId = :tenantId and s.sessionTokenHash = :hash
           """)
    Optional<UserSession> findByTenantAndTokenHash(@Param("tenantId") UUID tenantId,
                                                   @Param("hash") String hash);

    @Modifying
    @Query("""
           update UserSession s set s.revokedAt = :now
           where s.tenantId = :tenantId and s.userId = :userId and s.revokedAt is null
           """)
    int revokeAllForUser(@Param("tenantId") UUID tenantId,
                         @Param("userId") UUID userId,
                         @Param("now") OffsetDateTime now);

    Optional<UserSession> findByTenantIdAndSessionTokenHash(UUID tenantId, String sessionTokenHash);

    Optional<UserSession> findBySessionTokenHash(String sessionTokenHash);

    List<UserSession> findAllByTenantIdAndUserIdAndRevokedAtIsNull(UUID tenantId, UUID userId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
        update UserSession s
           set s.revokedAt = :now
         where s.tenantId = :tenantId
           and s.userId = :userId
           and s.revokedAt is null
        """)
    void revokeAllActiveForUser(@Param("tenantId") UUID tenantId,
                               @Param("userId") UUID userId,
                               @Param("now") OffsetDateTime now,
                               @Param("actorId") UUID actorId /* si tu entidad requiere auditUpdate, no sirve en JPQL */);

    @Query("""
        select s
          from UserSession s
         where s.tenantId = :tenantId
           and s.userId in :userIds
           and s.revokedAt is null
        """)
    List<UserSession> findAllActiveByTenantIdAndUserIds(@Param("tenantId") UUID tenantId,
                                                        @Param("userIds") List<UUID> userIds);
}
