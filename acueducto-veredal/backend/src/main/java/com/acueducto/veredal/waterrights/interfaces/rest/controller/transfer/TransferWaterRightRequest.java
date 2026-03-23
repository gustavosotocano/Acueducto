package com.acueducto.veredal.waterrights.interfaces.rest.controller.transfer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.UUID;

public record TransferWaterRightRequest(
        @NotNull UUID newOwnerId,
        @NotNull LocalDate transferDate,
        @NotBlank @Size(max = 100) String transferReason,
        @Size(max = 1000) String notes
) {
}
