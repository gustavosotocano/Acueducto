package com.acueducto.veredal.portfolio.interfaces.rest.controller;

import java.math.BigDecimal;
import java.util.UUID;

public record OwnerBalanceResponse(
        UUID ownerId,
        BigDecimal billedAmount,
        BigDecimal paidAmount,
        BigDecimal outstandingAmount
) {
}

