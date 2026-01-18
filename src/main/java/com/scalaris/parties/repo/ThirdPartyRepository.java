package com.scalaris.parties.repo;

import com.scalaris.parties.domain.ThirdParty;
import com.scalaris.parties.domain.ThirdPartyKind;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.*;

public interface ThirdPartyRepository extends JpaRepository<ThirdParty, UUID> {

    Optional<ThirdParty> findByIdAndActiveTrue(UUID id);

    @Query("""
        select t from ThirdParty t
        where t.active = true
          and (:kind is null or t.kind = :kind)
          and (
              :q is null
              or lower(t.displayName) like lower(concat('%', :q, '%'))
              or lower(coalesce(t.legalName, '')) like lower(concat('%', :q, '%'))
              or lower(coalesce(t.email, '')) like lower(concat('%', :q, '%'))
              or lower(coalesce(t.documentNumber, '')) like lower(concat('%', :q, '%'))
          )
        order by t.displayName asc
        """)
    List<ThirdParty> searchActive(@Param("kind") ThirdPartyKind kind, @Param("q") String q);

    @Query("""
        select count(t) > 0 from ThirdParty t
        where t.active = true
          and t.email is not null
          and lower(t.email) = lower(:email)
          and (:excludeId is null or t.id <> :excludeId)
        """)
    boolean existsActiveEmail(@Param("email") String email, @Param("excludeId") UUID excludeId);

    @Query("""
        select count(t) > 0 from ThirdParty t
        where t.active = true
          and t.documentNumber is not null
          and lower(t.documentType) = lower(:docType)
          and lower(t.documentNumber) = lower(:docNumber)
          and (:excludeId is null or t.id <> :excludeId)
        """)
    boolean existsActiveDocument(@Param("docType") String docType,
                                 @Param("docNumber") String docNumber,
                                 @Param("excludeId") UUID excludeId);
}
