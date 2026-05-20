package com.acueducto.veredal.invoices.application.port;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record WaterRightBillingSnapshot(
        UUID id,
        UUID ownerId,
        String connectionType,
        String serviceStatus,
        BigDecimal baseTariff,
        LocalDate ownershipStartDate,
        LocalDate ownershipEndDate
) {
}
