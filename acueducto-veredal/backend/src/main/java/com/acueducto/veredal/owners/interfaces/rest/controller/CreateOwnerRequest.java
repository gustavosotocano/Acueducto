package com.acueducto.veredal.owners.interfaces.rest.controller;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateOwnerRequest(
        @NotBlank @Size(max = 30) String documentType,
        @NotBlank @Size(max = 50) String documentNumber,
        @NotBlank @Size(max = 100) String firstName,
        @NotBlank @Size(max = 100) String lastName,
        @Size(max = 100) String secondLastName,
        @Size(max = 30) String phone,
        @Email @Size(max = 150) String email
) {
}
