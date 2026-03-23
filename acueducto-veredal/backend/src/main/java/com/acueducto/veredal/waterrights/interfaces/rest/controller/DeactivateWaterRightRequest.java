package com.acueducto.veredal.waterrights.interfaces.rest.controller;

import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record DeactivateWaterRightRequest(
        @NotNull LocalDate ownershipEndDate
) {
}
