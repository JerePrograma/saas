package com.scalaris.modules.identity.infrastructure.jpa;

import com.scalaris.modules.identity.domain.entity.User;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserJpaRepository extends JpaRepository<User, UUID> {

    @Query("""
            select u
            from User u
            where u.tenantId = :tenantId
              and lower(u.email) = :emailLower
            """)
    Optional<User> findByTenantAndEmailLower(@Param("tenantId") UUID tenantId,
                                             @Param("emailLower") String emailLower);

    @Query("""
            select distinct u
            from User u
            left join fetch u.roles r
            left join fetch r.permissions p
            where u.id = :userId and u.tenantId = :tenantId
            """)
    Optional<User> fetchAuthGraph(@Param("tenantId") UUID tenantId,
                                  @Param("userId") UUID userId);

    boolean existsByTenantId(UUID tenantId);

    @Query("""
            select count(u.id) > 0
            from User u
            where u.tenantId = :tenantId and lower(u.email) = lower(:email)
            """)
    boolean existsByTenantIdAndEmailIgnoreCase(@Param("tenantId") UUID tenantId, @Param("email") String emailLower);

    @Query("""
            select count(distinct u.id)
            from User u
            join u.roles r
            join r.permissions p
            where u.tenantId = :tenantId
              and u.status = com.scalaris.modules.identity.domain.entity.User$UserStatus.ACTIVE
              and p = :permission
            """)
    long countActiveUsersWithPermission(@Param("tenantId") UUID tenantId, @Param("permission") String permission);

    @Query("""
    select distinct u from User u
    left join fetch u.roles r
    where u.tenantId = :tenantId
    """)
    List<User> fetchAllAuthGraphsByTenantId(@Param("tenantId") UUID tenantId);

    boolean existsByTenantIdAndEmailIgnoreCaseAndIdNot(UUID tenantId, String email, UUID idNot);
}
