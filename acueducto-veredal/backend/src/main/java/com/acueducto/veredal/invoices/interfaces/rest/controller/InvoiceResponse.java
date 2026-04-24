package com.acueducto.veredal.invoices.interfaces.rest.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record InvoiceResponse(
        UUID id,
        String tenantId,
        UUID waterRightId,
        UUID ownerId,
        String invoiceNumber,
        String status,
        LocalDate periodStart,
        LocalDate periodEnd,
        LocalDate issueDate,
        LocalDate dueDate,
        BigDecimal totalAmount,
        String notes,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}

