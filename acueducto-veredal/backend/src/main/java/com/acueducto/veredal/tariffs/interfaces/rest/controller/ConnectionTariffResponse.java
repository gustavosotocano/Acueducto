package com.acueducto.veredal.tariffs.interfaces.rest.controller;

import com.acueducto.veredal.waterrights.domain.ConnectionType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record ConnectionTariffResponse(
        UUID id,
        String tenantId,
        ConnectionType connectionType,
        BigDecimal amount,
        LocalDate validFrom,
        LocalDate validTo,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}

