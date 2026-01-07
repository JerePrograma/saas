package com.scalaris.modules.identity.infrastructure.jpa;

import com.scalaris.modules.identity.domain.entity.TenantKey;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface TenantKeyJpaRepository extends JpaRepository<TenantKey, UUID> {

    /**
     * Resuelve tenant_id por key (case-insensitive). Usado por TenantResolver.
     */
    @Query(value = """
            select tk.tenant_id
            from tenant_key tk
            where tk.active = true
              and tk.key_type = :keyType
              and lower(tk.key_value) = lower(:keyValue)
            limit 1
            """, nativeQuery = true)
    Optional<UUID> findTenantIdByKeyValueCi(@Param("keyType") String keyType,
                                            @Param("keyValue") String keyValue);

    @Query(value = """
            select *
            from tenant_key tk
            where tk.active = true
              and tk.tenant_id = :tenantId
              and tk.key_type = :keyType
            limit 1
            """, nativeQuery = true)
    Optional<TenantKey> findActiveByTenantAndType(@Param("tenantId") UUID tenantId,
                                                  @Param("keyType") String keyType);

    @Query(value = """
            select exists(
                select 1
                from tenant_key tk
                where tk.active = true
                  and tk.key_type = :keyType
                  and lower(tk.key_value) = lower(:keyValue)
            )
            """, nativeQuery = true)
    boolean existsActiveByTypeAndValueCi(@Param("keyType") String keyType,
                                         @Param("keyValue") String keyValue);
}
