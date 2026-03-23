package com.acueducto.veredal.properties.interfaces.rest.controller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record CreatePropertyRequest(
        @NotBlank @Size(max = 50) String propertyCode,
        @NotBlank @Size(max = 200) String propertyName,
        @NotBlank @Size(max = 250) String addressLine,
        @NotNull UUID ownerId,
        @NotNull LocalDate ownershipStartDate
) {
}
