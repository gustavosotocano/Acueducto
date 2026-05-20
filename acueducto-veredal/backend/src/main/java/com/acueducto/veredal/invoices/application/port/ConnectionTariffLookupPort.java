package com.acueducto.veredal.invoices.application.port;

import java.time.LocalDate;
import java.util.Optional;

public interface ConnectionTariffLookupPort {

    Optional<ApplicableTariffSnapshot> findApplicable(String tenantId, String connectionType, LocalDate onDate);
}
