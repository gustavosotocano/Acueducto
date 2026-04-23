package com.acueducto.veredal.portfolio.interfaces.rest.controller;

import java.math.BigDecimal;

public record OverdueBucketResponse(
        String bucket,
        int invoiceCount,
        BigDecimal outstandingAmount
) {
}

