package com.acueducto.veredal.waterrights.interfaces.rest.controller.transfer;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

public record WaterRightOwnerHistoryResponse(
        UUID id,
        String tenantId,
        UUID waterRightId,
        UUID ownerId,
        LocalDate startDate,
        LocalDate endDate,
        String transferReason,
        String notes,
        OffsetDateTime createdAt
) {
}
