package com.acueducto.veredal.tariffs.interfaces.rest.controller;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDate;

public record UpdateConnectionTariffRequest(
        @NotNull @DecimalMin("0.00") BigDecimal amount,
        @NotNull LocalDate validFrom,
        LocalDate validTo
) {
}

