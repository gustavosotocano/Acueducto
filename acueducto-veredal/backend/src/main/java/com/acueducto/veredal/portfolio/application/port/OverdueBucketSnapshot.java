package com.acueducto.veredal.portfolio.application.port;

import java.math.BigDecimal;

public record OverdueBucketSnapshot(
        String bucket,
        int invoiceCount,
        BigDecimal outstandingAmount
) {
}
