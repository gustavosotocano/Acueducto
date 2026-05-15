package com.acueducto.veredal.auth.domain;

public enum UserRole {
    ADMIN,
    PRESIDENT,
    TREASURY;

    public static UserRole fromCode(String code) {
        if (code == null || code.isBlank()) {
            throw new IllegalArgumentException("Role is required");
        }
        return UserRole.valueOf(code.trim().toUpperCase());
    }
}
