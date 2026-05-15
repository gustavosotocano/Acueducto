package com.acueducto.veredal.auth.application;

import com.acueducto.veredal.auth.domain.UserRole;
import com.acueducto.veredal.auth.infrastructure.persistence.jpa.AppUserEntity;
import com.acueducto.veredal.auth.infrastructure.persistence.jpa.AppUserRepository;
import com.acueducto.veredal.shared.multitenancy.TenantContext;
import com.acueducto.veredal.tenants.infrastructure.persistence.jpa.TenantRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class UserService {

    private final AppUserRepository appUserRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(
            AppUserRepository appUserRepository,
            TenantRepository tenantRepository,
            PasswordEncoder passwordEncoder
    ) {
        this.appUserRepository = appUserRepository;
        this.tenantRepository = tenantRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public AppUserEntity create(CreateUserCommand command) {
        validatePassword(command.password());

        if (appUserRepository.existsByEmailIgnoreCase(command.email())) {
            throw new IllegalArgumentException("Email already registered");
        }

        String tenantId = resolveTenantId(command.tenantId(), command.roleCode());
        assertTenantExistsIfPresent(tenantId);

        OffsetDateTime now = OffsetDateTime.now();
        AppUserEntity user = new AppUserEntity();
        user.setId(UUID.randomUUID());
        user.setTenantId(tenantId);
        user.setFullName(command.fullName().trim());
        user.setEmail(command.email().trim().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(command.password()));
        user.setRoleCode(command.roleCode().trim().toUpperCase());
        user.setStatus("ACTIVE");
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        return appUserRepository.save(user);
    }

    @Transactional(readOnly = true)
    public List<AppUserEntity> listForCurrentTenant() {
        String tenantId = TenantContext.getRequiredTenantId();
        return appUserRepository.findAllByTenantIdOrderByFullNameAsc(tenantId);
    }

    @Transactional(readOnly = true)
    public AppUserEntity findById(UUID id) {
        String tenantId = TenantContext.getRequiredTenantId();
        return appUserRepository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    public AppUserEntity update(UUID id, UpdateUserCommand command) {
        AppUserEntity user = findById(id);
        OffsetDateTime now = OffsetDateTime.now();

        if (command.fullName() != null && !command.fullName().isBlank()) {
            user.setFullName(command.fullName().trim());
        }
        if (command.roleCode() != null && !command.roleCode().isBlank()) {
            UserRole.fromCode(command.roleCode());
            user.setRoleCode(command.roleCode().trim().toUpperCase());
        }
        if (command.status() != null && !command.status().isBlank()) {
            String status = command.status().trim().toUpperCase();
            if (!status.equals("ACTIVE") && !status.equals("INACTIVE")) {
                throw new IllegalArgumentException("Invalid status");
            }
            user.setStatus(status);
        }
        if (command.password() != null && !command.password().isBlank()) {
            validatePassword(command.password());
            user.setPasswordHash(passwordEncoder.encode(command.password()));
        }

        user.setUpdatedAt(now);
        return appUserRepository.save(user);
    }

    public AppUserEntity adminResetPassword(UUID id, String newPassword) {
        validatePassword(newPassword);
        AppUserEntity user = findById(id);
        OffsetDateTime now = OffsetDateTime.now();
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(now);
        return appUserRepository.save(user);
    }

    private String resolveTenantId(String requestedTenantId, String roleCode) {
        UserRole role = UserRole.fromCode(roleCode);
        if (role == UserRole.ADMIN && (requestedTenantId == null || requestedTenantId.isBlank())) {
            return null;
        }
        if (requestedTenantId != null && !requestedTenantId.isBlank()) {
            return requestedTenantId.trim();
        }
        return TenantContext.getRequiredTenantId();
    }

    private void assertTenantExistsIfPresent(String tenantId) {
        if (tenantId == null) {
            return;
        }
        boolean exists = tenantRepository.findByIdAndStatus(tenantId, "ACTIVE").isPresent();
        if (!exists) {
            throw new IllegalArgumentException("Tenant not found or inactive");
        }
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 8) {
            throw new IllegalArgumentException("Password must be at least 8 characters");
        }
    }

    public record CreateUserCommand(String fullName, String email, String password, String roleCode, String tenantId) {
    }

    public record UpdateUserCommand(String fullName, String roleCode, String status, String password) {
    }
}
