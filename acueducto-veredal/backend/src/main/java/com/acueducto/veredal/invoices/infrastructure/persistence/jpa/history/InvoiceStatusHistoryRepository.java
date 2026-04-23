package com.acueducto.veredal.invoices.infrastructure.persistence.jpa.history;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InvoiceStatusHistoryRepository extends JpaRepository<InvoiceStatusHistoryEntity, UUID> {
    List<InvoiceStatusHistoryEntity> findAllByTenantIdAndInvoiceIdOrderByChangedAtDesc(String tenantId, UUID invoiceId);
}

