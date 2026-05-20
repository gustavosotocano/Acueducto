package com.acueducto.veredal.invoices.application.port;

import java.math.BigDecimal;
import java.time.LocalDate;

public record ApplicableTariffSnapshot(
        BigDecimal amount,
        LocalDate validFrom,
        LocalDate validTo
) {
}
