package com.acueducto.veredal.invoices.application;

import com.acueducto.veredal.invoices.infrastructure.persistence.jpa.InvoiceEntity;
import com.acueducto.veredal.invoices.infrastructure.persistence.jpa.InvoiceRepository;
import com.acueducto.veredal.invoices.interfaces.rest.controller.CreateInvoiceRequest;
import com.acueducto.veredal.shared.multitenancy.TenantContext;
import com.acueducto.veredal.tariffs.application.ConnectionTariffService;
import com.acueducto.veredal.tariffs.infrastructure.persistence.jpa.ConnectionTariffEntity;
import com.acueducto.veredal.waterrights.infrastructure.persistence.jpa.WaterRightEntity;
import com.acueducto.veredal.waterrights.infrastructure.persistence.jpa.WaterRightRepository;
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
    private final WaterRightRepository waterRightRepository;
    private final ConnectionTariffService connectionTariffService;

    public InvoiceService(
            InvoiceRepository invoiceRepository,
            WaterRightRepository waterRightRepository,
            ConnectionTariffService connectionTariffService
    ) {
        this.invoiceRepository = invoiceRepository;
        this.waterRightRepository = waterRightRepository;
        this.connectionTariffService = connectionTariffService;
    }

    public InvoiceEntity create(CreateInvoiceRequest request) {
        String tenantId = TenantContext.getRequiredTenantId();
        WaterRightEntity waterRight = waterRightRepository.findByIdAndTenantId(request.waterRightId(), tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Water right not found"));

        validateDates(request.periodStart(), request.periodEnd(), request.issueDate(), request.dueDate());

        BigDecimal computedAmount = computeTotalAmount(waterRight, request.issueDate(), request.totalAmount());

        OffsetDateTime now = OffsetDateTime.now();
        InvoiceEntity invoice = new InvoiceEntity();
        invoice.setId(UUID.randomUUID());
        invoice.setTenantId(tenantId);
        invoice.setWaterRightId(waterRight.getId());
        invoice.setOwnerId(waterRight.getOwnerId());
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

    private BigDecimal computeTotalAmount(WaterRightEntity waterRight, LocalDate issueDate, BigDecimal requested) {
        // Preferred: use configured tariff by connection type and validity.
        try {
            ConnectionTariffEntity applicable = connectionTariffService.findApplicable(waterRight.getConnectionType(), issueDate);
            return applicable.getAmount();
        } catch (IllegalArgumentException ignored) {
            // Transitional fallback: keep system usable when tariffs haven't been configured yet.
            // If caller provided a total, accept it; otherwise use the water_right baseTariff.
            if (requested != null) return requested;
            return waterRight.getBaseTariff();
        }
    }
}

