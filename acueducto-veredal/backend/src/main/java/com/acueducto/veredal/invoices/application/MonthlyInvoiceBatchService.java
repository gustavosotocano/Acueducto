package com.acueducto.veredal.invoices.application;

import com.acueducto.veredal.invoices.infrastructure.persistence.jpa.InvoiceRepository;
import com.acueducto.veredal.invoices.interfaces.rest.controller.BatchMonthlyInvoiceRequest;
import com.acueducto.veredal.invoices.interfaces.rest.controller.BatchMonthlyInvoiceResponse;
import com.acueducto.veredal.invoices.interfaces.rest.controller.BatchMonthlyInvoiceResponse.BatchSkip;
import com.acueducto.veredal.shared.multitenancy.TenantContext;
import com.acueducto.veredal.waterrights.infrastructure.persistence.jpa.WaterRightEntity;
import com.acueducto.veredal.waterrights.infrastructure.persistence.jpa.WaterRightRepository;
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

    private final WaterRightRepository waterRightRepository;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceService invoiceService;
    private final InvoiceNumberAllocator invoiceNumberAllocator;

    public MonthlyInvoiceBatchService(
            WaterRightRepository waterRightRepository,
            InvoiceRepository invoiceRepository,
            InvoiceService invoiceService,
            InvoiceNumberAllocator invoiceNumberAllocator
    ) {
        this.waterRightRepository = waterRightRepository;
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

        List<WaterRightEntity> candidates = waterRightRepository.findAllByTenantIdAndServiceStatusOrderByRightCodeAsc(
                tenantId,
                "ACTIVE"
        );

        for (WaterRightEntity wr : candidates) {
            if (!isOwnershipActiveForPeriod(wr, periodStart, periodEnd)) {
                skipped.add(new BatchSkip(wr.getId(), "OWNERSHIP_NOT_ACTIVE_FOR_PERIOD"));
                continue;
            }
            if (invoiceRepository.existsByTenantIdAndWaterRightIdAndPeriodStartAndPeriodEndAndStatusNot(
                    tenantId,
                    wr.getId(),
                    periodStart,
                    periodEnd,
                    STATUS_CANCELLED
            )) {
                skipped.add(new BatchSkip(wr.getId(), "INVOICE_ALREADY_EXISTS_FOR_PERIOD"));
                continue;
            }
            try {
                String invoiceNumber = invoiceNumberAllocator.allocateNext(tenantId);
                String notes = "Monthly batch " + ym;
                var created = invoiceService.createForBatch(
                        wr.getId(),
                        invoiceNumber,
                        periodStart,
                        periodEnd,
                        issueDate,
                        dueDate,
                        notes
                );
                createdIds.add(created.getId());
            } catch (DataIntegrityViolationException ex) {
                skipped.add(new BatchSkip(wr.getId(), "DUPLICATE_PREVENTED_AT_PERSISTENCE"));
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

    private boolean isOwnershipActiveForPeriod(WaterRightEntity wr, LocalDate periodStart, LocalDate periodEnd) {
        if (wr.getOwnershipStartDate().isAfter(periodEnd)) {
            return false;
        }
        if (wr.getOwnershipEndDate() != null && wr.getOwnershipEndDate().isBefore(periodStart)) {
            return false;
        }
        return true;
    }
}
