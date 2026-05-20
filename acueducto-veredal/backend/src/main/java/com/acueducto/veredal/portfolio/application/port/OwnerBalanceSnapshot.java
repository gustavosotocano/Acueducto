package com.acueducto.veredal.portfolio.application.port;

import java.math.BigDecimal;
import java.util.UUID;

public record OwnerBalanceSnapshot(
        UUID ownerId,
        BigDecimal billedAmount,
        BigDecimal paidAmount,
        BigDecimal outstandingAmount
) {
}
