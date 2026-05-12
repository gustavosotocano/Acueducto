package com.acueducto.veredal.invoices.interfaces.rest.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record BatchMonthlyInvoiceResponse(
        int billingYear,
        int billingMonth,
        LocalDate periodStart,
        LocalDate periodEnd,
        LocalDate issueDate,
        LocalDate dueDate,
        int createdCount,
        int skippedCount,
        List<UUID> createdInvoiceIds,
        List<BatchSkip> skipped
) {
    public record BatchSkip(UUID waterRightId, String reason) {
    }
}
