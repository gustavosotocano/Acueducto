package com.acueducto.veredal.tariffs.interfaces.rest.controller;

import com.acueducto.veredal.waterrights.domain.ConnectionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record CreateConnectionTariffRequest(
        @NotNull ConnectionType connectionType,
        @NotNull @DecimalMin("0.00") BigDecimal amount,
        @NotNull LocalDate validFrom,
        LocalDate validTo
) {
}

