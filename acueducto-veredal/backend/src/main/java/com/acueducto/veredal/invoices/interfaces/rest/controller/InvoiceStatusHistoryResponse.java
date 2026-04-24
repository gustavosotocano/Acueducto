package com.acueducto.veredal.invoices.interfaces.rest.controller;

import java.time.OffsetDateTime;
import java.util.UUID;

public record InvoiceStatusHistoryResponse(
        UUID id,
        String tenantId,
        UUID invoiceId,
        String fromStatus,
        String toStatus,
        String note,
        OffsetDateTime changedAt
) {
}

