package com.acueducto.veredal.invoices.application;

import com.acueducto.veredal.invoices.application.port.ApplicableTariffSnapshot;
import com.acueducto.veredal.invoices.application.port.ConnectionTariffLookupPort;
import com.acueducto.veredal.invoices.application.port.WaterRightBillingPort;
import com.acueducto.veredal.invoices.application.port.WaterRightBillingSnapshot;
import com.acueducto.veredal.invoices.infrastructure.persistence.jpa.InvoiceEntity;
import com.acueducto.veredal.invoices.infrastructure.persistence.jpa.InvoiceRepository;
import com.acueducto.veredal.invoices.interfaces.rest.controller.CreateInvoiceRequest;
import com.acueducto.veredal.shared.multitenancy.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class InvoiceService {

    private final InvoiceRepository invoiceRepository;
    private final WaterRightBillingPort waterRightBillingPort;
    private final ConnectionTariffLookupPort connectionTariffLookupPort;

    public InvoiceService(
            InvoiceRepository invoiceRepository,
            WaterRightBillingPort waterRightBillingPort,
            ConnectionTariffLookupPort connectionTariffLookupPort
    ) {
        this.invoiceRepository = invoiceRepository;
        this.waterRightBillingPort = waterRightBillingPort;
        this.connectionTariffLookupPort = connectionTariffLookupPort;
    }

    public InvoiceEntity create(CreateInvoiceRequest request) {
        String tenantId = TenantContext.getRequiredTenantId();
        WaterRightBillingSnapshot waterRight = waterRightBillingPort.findForBilling(tenantId, request.waterRightId());

        validateDates(request.periodStart(), request.periodEnd(), request.issueDate(), request.dueDate());

        BigDecimal computedAmount = computeTotalAmount(waterRight, request.issueDate(), request.totalAmount());

        OffsetDateTime now = OffsetDateTime.now();
        InvoiceEntity invoice = new InvoiceEntity();
        invoice.setId(UUID.randomUUID());
        invoice.setTenantId(tenantId);
        invoice.setWaterRightId(waterRight.id());
        invoice.setOwnerId(waterRight.ownerId());
        invoice.setInvoiceNumber(request.invoiceNumber());
        invoice.setStatus("ISSUED");
        invoice.setPeriodStart(request.periodStart());
        invoice.setPeriodEnd(request.periodEnd());
        invoice.setIssueDate(request.issueDate());
        invoice.setDueDate(request.dueDate());
        invoice.setTotalAmount(computedAmount);
        invoice.setNotes(request.notes());
        invoice.setCreatedAt(now);
        invoice.setUpdatedAt(now);
        return invoiceRepository.save(invoice);
    }

    public InvoiceEntity createForBatch(
            UUID waterRightId,
            String invoiceNumber,
            LocalDate periodStart,
            LocalDate periodEnd,
            LocalDate issueDate,
            LocalDate dueDate,
            String notes
    ) {
        return create(new CreateInvoiceRequest(
                waterRightId,
                invoiceNumber,
                periodStart,
                periodEnd,
                issueDate,
                dueDate,
                null,
                notes
        ));
    }

    @Transactional(readOnly = true)
    public InvoiceEntity findById(UUID id) {
        return invoiceRepository.findByIdAndTenantId(id, TenantContext.getRequiredTenantId())
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found"));
    }

    @Transactional(readOnly = true)
    public List<InvoiceEntity> findAll(UUID waterRightId, UUID ownerId, String status, LocalDate issueFrom, LocalDate issueTo) {
        String tenantId = TenantContext.getRequiredTenantId();

        if (waterRightId != null) {
            return invoiceRepository.findAllByTenantIdAndWaterRightIdOrderByIssueDateDesc(tenantId, waterRightId);
        }
        if (ownerId != null) {
            return invoiceRepository.findAllByTenantIdAndOwnerIdOrderByIssueDateDesc(tenantId, ownerId);
        }
        if (status != null && issueFrom != null && issueTo != null) {
            return invoiceRepository.findAllByTenantIdAndStatusAndIssueDateBetweenOrderByIssueDateDesc(tenantId, status, issueFrom, issueTo);
        }
        if (status != null) {
            return invoiceRepository.findAllByTenantIdAndStatusOrderByIssueDateDesc(tenantId, status);
        }
        if (issueFrom != null && issueTo != null) {
            return invoiceRepository.findAllByTenantIdAndIssueDateBetweenOrderByIssueDateDesc(tenantId, issueFrom, issueTo);
        }

        return invoiceRepository.findAllByTenantIdOrderByIssueDateDesc(tenantId);
    }

    private void validateDates(LocalDate periodStart, LocalDate periodEnd, LocalDate issueDate, LocalDate dueDate) {
        if (periodEnd.isBefore(periodStart)) {
            throw new IllegalArgumentException("Invoice period end cannot be before period start");
        }
        if (dueDate.isBefore(issueDate)) {
            throw new IllegalArgumentException("Due date cannot be before issue date");
        }
    }

    private BigDecimal computeTotalAmount(WaterRightBillingSnapshot waterRight, LocalDate issueDate, BigDecimal requested) {
        String tenantId = TenantContext.getRequiredTenantId();
        return connectionTariffLookupPort
                .findApplicable(tenantId, waterRight.connectionType(), issueDate)
                .map(ApplicableTariffSnapshot::amount)
                .orElseGet(() -> requested != null ? requested : waterRight.baseTariff());
    }
}
