package com.acueducto.veredal.properties.interfaces.rest.controller.ownership;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record ChangePropertyOwnerRequest(
        @NotNull UUID newOwnerId,
        @NotNull LocalDate changeDate,
        @Size(max = 100) String changeReason,
        String notes
) {
}
