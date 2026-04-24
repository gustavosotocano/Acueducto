package com.acueducto.veredal.owners.interfaces.rest.controller;

import java.time.OffsetDateTime;
import java.util.UUID;

public record OwnerResponse(
        UUID id,
        String tenantId,
        String documentType,
        String documentNumber,
        String fullName,
        String firstName,
        String lastName,
        String secondLastName,
        String phone,
        String email,
        String status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
