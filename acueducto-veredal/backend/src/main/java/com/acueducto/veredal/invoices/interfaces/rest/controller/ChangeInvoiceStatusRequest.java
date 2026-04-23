package com.acueducto.veredal.invoices.interfaces.rest.controller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangeInvoiceStatusRequest(
        @NotBlank @Size(max = 20) String status,
        @Size(max = 250) String note
) {
}

