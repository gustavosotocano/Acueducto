package com.acueducto.veredal.payments.interfaces.rest.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PaymentResponse(
        UUID id,
        String tenantId,
        UUID invoiceId,
        UUID waterRightId,
        UUID ownerId,
        LocalDate paymentDate,
        BigDecimal amount,
        String method,
        String reference,
        String notes,
        OffsetDateTime createdAt
) {
}

