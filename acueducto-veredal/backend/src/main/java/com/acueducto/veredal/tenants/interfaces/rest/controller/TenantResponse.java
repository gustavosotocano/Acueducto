package com.acueducto.veredal.tenants.interfaces.rest.controller;

import java.time.OffsetDateTime;

public record TenantResponse(
        String id,
        String tenantName,
        String contactName,
        String contactPhone,
        String contactEmail,
        String status,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}

