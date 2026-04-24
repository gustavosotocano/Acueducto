package com.acueducto.veredal.portfolio.interfaces.rest.controller;

import java.math.BigDecimal;

public record OverdueBucketWithInterestResponse(
        String bucket,
        int invoiceCount,
        BigDecimal outstandingAmount,
        BigDecimal interestAmount,
        BigDecimal totalWithInterest
) {
}

