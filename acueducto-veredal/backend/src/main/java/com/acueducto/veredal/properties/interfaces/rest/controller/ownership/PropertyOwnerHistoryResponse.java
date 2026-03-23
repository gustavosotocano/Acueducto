package com.acueducto.veredal.properties.interfaces.rest.controller.ownership;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PropertyOwnerHistoryResponse(
        UUID id,
        String tenantId,
        UUID propertyId,
        UUID ownerId,
        LocalDate startDate,
        LocalDate endDate,
        String changeReason,
        String notes,
        OffsetDateTime createdAt
) {
}
