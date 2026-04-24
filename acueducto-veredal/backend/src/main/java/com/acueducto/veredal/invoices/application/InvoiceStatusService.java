package com.acueducto.veredal.invoices.application;

import com.acueducto.veredal.invoices.infrastructure.persistence.jpa.InvoiceEntity;
import com.acueducto.veredal.invoices.infrastructure.persistence.jpa.InvoiceRepository;
import com.acueducto.veredal.invoices.infrastructure.persistence.jpa.history.InvoiceStatusHistoryEntity;
import com.acueducto.veredal.invoices.infrastructure.persistence.jpa.history.InvoiceStatusHistoryRepository;
import com.acueducto.veredal.shared.multitenancy.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class InvoiceStatusService {

    private static final Set<String> ALLOWED_STATUSES = Set.of("DRAFT", "ISSUED", "PAID", "OVERDUE", "CANCELLED");

    private final InvoiceRepository invoiceRepository;
    private final InvoiceStatusHistoryRepository invoiceStatusHistoryRepository;

    public InvoiceStatusService(
            InvoiceRepository invoiceRepository,
            InvoiceStatusHistoryRepository invoiceStatusHistoryRepository
    ) {
        this.invoiceRepository = invoiceRepository;
        this.invoiceStatusHistoryRepository = invoiceStatusHistoryRepository;
    }

    public InvoiceEntity changeStatus(UUID invoiceId, String toStatus, String note) {
        String tenantId = TenantContext.getRequiredTenantId();
        InvoiceEntity invoice = invoiceRepository.findByIdAndTenantId(invoiceId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found"));

        String normalized = normalizeStatus(toStatus);
        if (!ALLOWED_STATUSES.contains(normalized)) {
            throw new IllegalArgumentException("Invalid invoice status");
        }

        String from = invoice.getStatus();
        if (from != null && from.equalsIgnoreCase(normalized)) {
            return invoice;
        }

        OffsetDateTime now = OffsetDateTime.now();
        invoice.setStatus(normalized);
        invoice.setUpdatedAt(now);
        InvoiceEntity saved = invoiceRepository.save(invoice);

        InvoiceStatusHistoryEntity history = new InvoiceStatusHistoryEntity();
        history.setId(UUID.randomUUID());
        history.setTenantId(tenantId);
        history.setInvoiceId(invoiceId);
        history.setFromStatus(from);
        history.setToStatus(normalized);
        history.setChangeNote(note);
        history.setChangedAt(now);
        invoiceStatusHistoryRepository.save(history);

        return saved;
    }

    @Transactional(readOnly = true)
    public List<InvoiceStatusHistoryEntity> history(UUID invoiceId) {
        String tenantId = TenantContext.getRequiredTenantId();
        invoiceRepository.findByIdAndTenantId(invoiceId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found"));
        return invoiceStatusHistoryRepository.findAllByTenantIdAndInvoiceIdOrderByChangedAtDesc(tenantId, invoiceId);
    }

    private String normalizeStatus(String value) {
        if (value == null) return "";
        return value.trim().toUpperCase();
    }
}

