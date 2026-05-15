package com.acueducto.veredal.auth.application;

import com.acueducto.veredal.auth.infrastructure.persistence.jpa.AppUserEntity;
import com.acueducto.veredal.auth.infrastructure.persistence.jpa.AppUserRepository;
import com.acueducto.veredal.auth.infrastructure.persistence.jpa.PasswordResetTokenEntity;
import com.acueducto.veredal.auth.infrastructure.persistence.jpa.PasswordResetTokenRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.UUID;

@Service
@Transactional
public class AuthService {

    private final AppUserRepository appUserRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final long passwordResetExpirationHours;

    public AuthService(
            AppUserRepository appUserRepository,
            PasswordResetTokenRepository passwordResetTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtTokenService jwtTokenService,
            @Value("${app.security.password-reset.expiration-hours:24}") long passwordResetExpirationHours
    ) {
        this.appUserRepository = appUserRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenService = jwtTokenService;
        this.passwordResetExpirationHours = passwordResetExpirationHours;
    }

    public LoginResult login(String email, String password) {
        AppUserEntity user = appUserRepository.findByEmailIgnoreCase(email.trim())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        if (!"ACTIVE".equalsIgnoreCase(user.getStatus())) {
            throw new IllegalArgumentException("User is not active");
        }

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid email or password");
        }

        OffsetDateTime now = OffsetDateTime.now();
        user.setLastLoginAt(now);
        user.setUpdatedAt(now);
        appUserRepository.save(user);

        String token = jwtTokenService.createToken(user);
        return new LoginResult(token, toProfile(user));
    }

    @Transactional(readOnly = true)
    public UserProfile me(UUID userId) {
        AppUserEntity user = appUserRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        return toProfile(user);
    }

    public PasswordResetRequestResult requestPasswordReset(String email) {
        appUserRepository.findByEmailIgnoreCase(email.trim()).ifPresent(user -> {
            String rawToken = generateResetToken();
            OffsetDateTime now = OffsetDateTime.now();

            PasswordResetTokenEntity entity = new PasswordResetTokenEntity();
            entity.setId(UUID.randomUUID());
            entity.setUserId(user.getId());
            entity.setTokenHash(hashToken(rawToken));
            entity.setExpiresAt(now.plusHours(passwordResetExpirationHours));
            entity.setCreatedAt(now);
            passwordResetTokenRepository.save(entity);
        });

        return new PasswordResetRequestResult(
                "If the email exists, a reset token was generated. Use it before it expires."
        );
    }

    public PasswordResetResult resetPassword(String rawToken, String newPassword) {
        validatePassword(newPassword);

        PasswordResetTokenEntity token = passwordResetTokenRepository
                .findByTokenHashAndUsedAtIsNull(hashToken(rawToken.trim()))
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired reset token"));

        if (token.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new IllegalArgumentException("Invalid or expired reset token");
        }

        AppUserEntity user = appUserRepository.findById(token.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        OffsetDateTime now = OffsetDateTime.now();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(now);
        appUserRepository.save(user);

        token.setUsedAt(now);
        passwordResetTokenRepository.save(token);

        return new PasswordResetResult("Password updated successfully");
    }

    private UserProfile toProfile(AppUserEntity user) {
        return new UserProfile(
                user.getId(),
                user.getTenantId(),
                user.getFullName(),
                user.getEmail(),
                user.getRoleCode(),
                user.getStatus()
        );
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters");
        }
    }

    private String generateResetToken() {
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hashed);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    public record LoginResult(String accessToken, UserProfile user) {
    }

    public record UserProfile(UUID id, String tenantId, String fullName, String email, String role, String status) {
    }

    public record PasswordResetRequestResult(String message) {
    }

    public record PasswordResetResult(String message) {
    }
}
