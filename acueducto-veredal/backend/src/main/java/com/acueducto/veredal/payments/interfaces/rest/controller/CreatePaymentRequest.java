package com.acueducto.veredal.payments.interfaces.rest.controller;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CreatePaymentRequest(
        @NotNull UUID invoiceId,
        @NotNull LocalDate paymentDate,
        @NotNull @DecimalMin("0.01") BigDecimal amount,
        @NotBlank @Size(max = 30) String method,
        @Size(max = 100) String reference,
        @Size(max = 2000) String notes
) {
}

