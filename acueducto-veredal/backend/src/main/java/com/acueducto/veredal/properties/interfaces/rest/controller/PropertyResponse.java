package com.acueducto.veredal.properties.interfaces.rest.controller;

import java.time.OffsetDateTime;
import java.util.UUID;

public record PropertyResponse(
        UUID id,
        String tenantId,
        String propertyCode,
        String propertyName,
        String addressLine,
        UUID ownerId,
        String status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
