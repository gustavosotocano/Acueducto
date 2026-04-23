package com.acueducto.veredal.invoices.interfaces.rest.controller;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreateInvoiceRequest(
        @NotNull UUID waterRightId,
        @NotBlank @Size(max = 50) String invoiceNumber,
        @NotNull LocalDate periodStart,
        @NotNull LocalDate periodEnd,
        @NotNull LocalDate issueDate,
        @NotNull LocalDate dueDate,
        @NotNull @DecimalMin("0.00") BigDecimal totalAmount,
        @Size(max = 2000) String notes
) {
}

