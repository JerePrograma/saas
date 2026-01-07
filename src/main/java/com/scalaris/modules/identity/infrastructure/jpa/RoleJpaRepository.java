package com.scalaris.modules.identity.infrastructure.jpa;

import com.scalaris.modules.identity.domain.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RoleJpaRepository extends JpaRepository<Role, UUID> {

    Optional<Role> findByTenantIdAndName(UUID tenantId, String name);

    boolean existsByTenantIdAndName(UUID tenantId, String name);

    @Query("""
           select r
           from Role r
           where r.tenantId = :tenantId
           order by r.name asc
           """)
    List<Role> findAllByTenantId(@Param("tenantId") UUID tenantId);

    @Query(value = """
        select ur.user_id
        from user_role ur
        join app_user u on u.id = ur.user_id
        where ur.role_id = :roleId
          and u.tenant_id = :tenantId
        """, nativeQuery = true)
    List<UUID> findUserIdsByTenantIdAndRoleId(@Param("tenantId") UUID tenantId,
                                              @Param("roleId") UUID roleId);
}
