package com.acueducto.veredal.invoices.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InvoiceRepository extends JpaRepository<InvoiceEntity, UUID> {
    Optional<InvoiceEntity> findByIdAndTenantId(UUID id, String tenantId);

    List<InvoiceEntity> findAllByTenantIdOrderByIssueDateDesc(String tenantId);

    List<InvoiceEntity> findAllByTenantIdAndWaterRightIdOrderByIssueDateDesc(String tenantId, UUID waterRightId);

    List<InvoiceEntity> findAllByTenantIdAndOwnerIdOrderByIssueDateDesc(String tenantId, UUID ownerId);

    List<InvoiceEntity> findAllByTenantIdAndStatusOrderByIssueDateDesc(String tenantId, String status);

    List<InvoiceEntity> findAllByTenantIdAndIssueDateBetweenOrderByIssueDateDesc(String tenantId, LocalDate from, LocalDate to);

    List<InvoiceEntity> findAllByTenantIdAndStatusAndIssueDateBetweenOrderByIssueDateDesc(
            String tenantId,
            String status,
            LocalDate from,
            LocalDate to
    );

    boolean existsByTenantIdAndWaterRightIdAndPeriodStartAndPeriodEndAndStatusNot(
            String tenantId,
            UUID waterRightId,
            LocalDate periodStart,
            LocalDate periodEnd,
            String status
    );
}

