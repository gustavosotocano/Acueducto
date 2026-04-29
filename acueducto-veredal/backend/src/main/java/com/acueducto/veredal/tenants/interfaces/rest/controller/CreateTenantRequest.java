package com.acueducto.veredal.tenants.interfaces.rest.controller;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateTenantRequest(
        @NotBlank @Size(max = 64) String id,
        @NotBlank @Size(max = 200) String tenantName,
        @Size(max = 200) String contactName,
        @Size(max = 30) String contactPhone,
        @Email @Size(max = 150) String contactEmail
) {
}

