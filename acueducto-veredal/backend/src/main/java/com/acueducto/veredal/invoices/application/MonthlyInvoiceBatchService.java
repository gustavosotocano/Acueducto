package com.acueducto.veredal.invoices.application;

import com.acueducto.veredal.invoices.application.port.WaterRightBillingPort;
import com.acueducto.veredal.invoices.application.port.WaterRightBillingSnapshot;
import com.acueducto.veredal.invoices.infrastructure.persistence.jpa.InvoiceRepository;
import com.acueducto.veredal.invoices.interfaces.rest.controller.BatchMonthlyInvoiceRequest;
import com.acueducto.veredal.invoices.interfaces.rest.controller.BatchMonthlyInvoiceResponse;
import com.acueducto.veredal.invoices.interfaces.rest.controller.BatchMonthlyInvoiceResponse.BatchSkip;
import com.acueducto.veredal.shared.multitenancy.TenantContext;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class MonthlyInvoiceBatchService {

    private static final String STATUS_CANCELLED = "CANCELLED";

    private final WaterRightBillingPort waterRightBillingPort;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceService invoiceService;
    private final InvoiceNumberAllocator invoiceNumberAllocator;

    public MonthlyInvoiceBatchService(
            WaterRightBillingPort waterRightBillingPort,
            InvoiceRepository invoiceRepository,
            InvoiceService invoiceService,
            InvoiceNumberAllocator invoiceNumberAllocator
    ) {
        this.waterRightBillingPort = waterRightBillingPort;
        this.invoiceRepository = invoiceRepository;
        this.invoiceService = invoiceService;
        this.invoiceNumberAllocator = invoiceNumberAllocator;
    }

    public BatchMonthlyInvoiceResponse run(BatchMonthlyInvoiceRequest request) {
        String tenantId = TenantContext.getRequiredTenantId();
        YearMonth ym = YearMonth.of(request.billingYear(), request.billingMonth());
        LocalDate periodStart = ym.atDay(1);
        LocalDate periodEnd = ym.atEndOfMonth();
        LocalDate issueDate = request.issueDate() != null ? request.issueDate() : periodEnd;
        LocalDate dueDate = request.dueDate() != null ? request.dueDate() : issueDate.plusDays(15);

        if (dueDate.isBefore(issueDate)) {
            throw new IllegalArgumentException("dueDate cannot be before issueDate");
        }

        List<UUID> createdIds = new ArrayList<>();
        List<BatchSkip> skipped = new ArrayList<>();

        List<WaterRightBillingSnapshot> candidates = waterRightBillingPort.findActiveForMonthlyBatch(tenantId);

        for (WaterRightBillingSnapshot wr : candidates) {
            if (!isOwnershipActiveForPeriod(wr, periodStart, periodEnd)) {
                skipped.add(new BatchSkip(wr.id(), "OWNERSHIP_NOT_ACTIVE_FOR_PERIOD"));
                continue;
            }
            if (invoiceRepository.existsByTenantIdAndWaterRightIdAndPeriodStartAndPeriodEndAndStatusNot(
                    tenantId,
                    wr.id(),
                    periodStart,
                    periodEnd,
                    STATUS_CANCELLED
            )) {
                skipped.add(new BatchSkip(wr.id(), "INVOICE_ALREADY_EXISTS_FOR_PERIOD"));
                continue;
            }
            try {
                String invoiceNumber = invoiceNumberAllocator.allocateNext(tenantId);
                String notes = "Monthly batch " + ym;
                var created = invoiceService.createForBatch(
                        wr.id(),
                        invoiceNumber,
                        periodStart,
                        periodEnd,
                        issueDate,
                        dueDate,
                        notes
                );
                createdIds.add(created.getId());
            } catch (DataIntegrityViolationException ex) {
                skipped.add(new BatchSkip(wr.id(), "DUPLICATE_PREVENTED_AT_PERSISTENCE"));
            }
        }

        return new BatchMonthlyInvoiceResponse(
                request.billingYear(),
                request.billingMonth(),
                periodStart,
                periodEnd,
                issueDate,
                dueDate,
                createdIds.size(),
                skipped.size(),
                createdIds,
                skipped
        );
    }

    private boolean isOwnershipActiveForPeriod(WaterRightBillingSnapshot wr, LocalDate periodStart, LocalDate periodEnd) {
        if (wr.ownershipStartDate().isAfter(periodEnd)) {
            return false;
        }
        if (wr.ownershipEndDate() != null && wr.ownershipEndDate().isBefore(periodStart)) {
            return false;
        }
        return true;
    }
}
