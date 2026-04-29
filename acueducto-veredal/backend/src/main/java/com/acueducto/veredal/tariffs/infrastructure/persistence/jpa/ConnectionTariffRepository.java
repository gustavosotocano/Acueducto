package com.acueducto.veredal.tariffs.infrastructure.persistence.jpa;

import com.acueducto.veredal.waterrights.domain.ConnectionType;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ConnectionTariffRepository extends JpaRepository<ConnectionTariffEntity, UUID> {

    List<ConnectionTariffEntity> findAllByTenantIdOrderByConnectionTypeAscValidFromDesc(String tenantId);

    Optional<ConnectionTariffEntity> findByIdAndTenantId(UUID id, String tenantId);

    @Query("""
            select ct
            from ConnectionTariffEntity ct
            where ct.tenantId = :tenantId
              and ct.connectionType = :connectionType
              and ct.validFrom <= :onDate
              and (ct.validTo is null or ct.validTo >= :onDate)
            order by ct.validFrom desc
            """)
    List<ConnectionTariffEntity> findApplicable(
            @Param("tenantId") String tenantId,
            @Param("connectionType") ConnectionType connectionType,
            @Param("onDate") LocalDate onDate,
            Pageable pageable
    );

    @Query("""
            select ct
            from ConnectionTariffEntity ct
            where ct.tenantId = :tenantId
              and ct.connectionType = :connectionType
              and ct.validFrom <= :to
              and coalesce(ct.validTo, :maxDate) >= :from
            """)
    List<ConnectionTariffEntity> findOverlaps(
            @Param("tenantId") String tenantId,
            @Param("connectionType") ConnectionType connectionType,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("maxDate") LocalDate maxDate
    );

    @Query("""
            select ct
            from ConnectionTariffEntity ct
            where ct.tenantId = :tenantId
              and ct.connectionType = :connectionType
              and ct.id <> :excludeId
              and ct.validFrom <= :to
              and coalesce(ct.validTo, :maxDate) >= :from
            """)
    List<ConnectionTariffEntity> findOverlapsExcludingId(
            @Param("tenantId") String tenantId,
            @Param("connectionType") ConnectionType connectionType,
            @Param("from") LocalDate from,
            @Param("to") LocalDate to,
            @Param("maxDate") LocalDate maxDate,
            @Param("excludeId") UUID excludeId
    );
}

