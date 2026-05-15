package com.acueducto.veredal.auth.interfaces.rest.controller;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.UUID;

record LoginRequest(
        @NotBlank @Email String email,
        @NotBlank String password
) {
}

record LoginResponse(String accessToken, AuthUserResponse user) {
}

record AuthUserResponse(
        UUID id,
        String tenantId,
        String fullName,
        String email,
        String role,
        String status
) {
}

record PasswordResetRequest(@NotBlank @Email String email) {
}

record PasswordResetConfirmRequest(
        @NotBlank String token,
        @NotBlank @Size(min = 8) String newPassword
) {
}
