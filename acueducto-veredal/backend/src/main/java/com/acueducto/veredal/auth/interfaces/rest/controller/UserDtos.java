package com.acueducto.veredal.auth.interfaces.rest.controller;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.OffsetDateTime;
import java.util.UUID;

record CreateUserRequest(
        @NotBlank String fullName,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8) String password,
        @NotBlank String roleCode,
        String tenantId
) {
}

record UpdateUserRequest(String fullName, String roleCode, String status, String password) {
}

record AdminResetPasswordRequest(@NotBlank @Size(min = 8) String newPassword) {
}

record UserResponse(
        UUID id,
        String tenantId,
        String fullName,
        String email,
        String roleCode,
        String status,
        OffsetDateTime lastLoginAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
