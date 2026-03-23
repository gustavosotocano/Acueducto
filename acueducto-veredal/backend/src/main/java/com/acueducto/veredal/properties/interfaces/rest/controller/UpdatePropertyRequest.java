package com.acueducto.veredal.properties.interfaces.rest.controller;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdatePropertyRequest(
        @NotBlank @Size(max = 200) String propertyName,
        @NotBlank @Size(max = 250) String addressLine
) {
}
