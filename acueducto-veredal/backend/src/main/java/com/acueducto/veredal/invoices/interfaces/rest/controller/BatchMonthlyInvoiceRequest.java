package com.acueducto.veredal.invoices.interfaces.rest.controller;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;

public record BatchMonthlyInvoiceRequest(
        @NotNull @Min(2000) @Max(3000) Integer billingYear,
        @NotNull @Min(1) @Max(12) Integer billingMonth,
        LocalDate issueDate,
        LocalDate dueDate
) {
}
