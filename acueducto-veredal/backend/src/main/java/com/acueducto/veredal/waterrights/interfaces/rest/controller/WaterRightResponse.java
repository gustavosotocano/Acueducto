package com.acueducto.veredal.waterrights.interfaces.rest.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record WaterRightResponse(
        UUID id,
        String tenantId,
        String rightCode,
        UUID ownerId,
        UUID propertyId,
        UUID waterMeterId,
        String serviceStatus,
        String connectionType,
        BigDecimal baseTariff,
        LocalDate ownershipStartDate,
        LocalDate ownershipEndDate,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
