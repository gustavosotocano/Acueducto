package com.acueducto.veredal.waterrights.interfaces.rest.controller;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreateWaterRightRequest(
        @NotBlank @Size(max = 50) String rightCode,
        @NotNull UUID ownerId,
        @NotNull UUID propertyId,
        UUID waterMeterId,
        @NotBlank @Size(max = 20) String serviceStatus,
        @NotBlank @Size(max = 30) String connectionType,
        @NotNull @DecimalMin("0.00") BigDecimal baseTariff,
        @NotNull LocalDate ownershipStartDate
) {
}
