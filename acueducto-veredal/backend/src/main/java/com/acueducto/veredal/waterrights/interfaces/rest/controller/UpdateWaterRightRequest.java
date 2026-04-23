package com.acueducto.veredal.waterrights.interfaces.rest.controller;

import com.acueducto.veredal.waterrights.domain.ConnectionType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

public record UpdateWaterRightRequest(
        @NotNull UUID propertyId,
        UUID waterMeterId,
        @NotBlank @Size(max = 20) String serviceStatus,
        @NotNull ConnectionType connectionType,
        @NotNull @DecimalMin("0.00") BigDecimal baseTariff
) {
}
