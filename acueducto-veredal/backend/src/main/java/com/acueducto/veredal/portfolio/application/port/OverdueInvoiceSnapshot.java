package com.acueducto.veredal.portfolio.application.port;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record OverdueInvoiceSnapshot(
        UUID invoiceId,
        LocalDate dueDate,
        BigDecimal outstandingAmount
) {
}
