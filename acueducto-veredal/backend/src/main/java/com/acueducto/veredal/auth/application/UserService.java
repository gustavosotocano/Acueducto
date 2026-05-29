package com.acueducto.veredal.auth.application;

import com.acueducto.veredal.auth.domain.UserRole;
import com.acueducto.veredal.auth.infrastructure.persistence.jpa.AppUserEntity;
import com.acueducto.veredal.auth.infrastructure.persistence.jpa.AppUserRepository;
import com.acueducto.veredal.shared.security.AuthenticatedUser;
import com.acueducto.veredal.shared.security.SecurityUtils;
import com.acueducto.veredal.tenants.infrastructure.persistence.jpa.TenantRepository;
import org.springframework.security.access.AccessDeniedException;
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
        AuthenticatedUser actor = SecurityUtils.currentUser();
        assertCanManageUsers(actor);

        validatePassword(command.password());
        UserRole role = UserRole.fromCode(command.roleCode());
        assertRoleAllowedOnCreate(actor, role);

        if (appUserRepository.existsByEmailIgnoreCase(command.email())) {
            throw new IllegalArgumentException("Email already registered");
        }

        String tenantId = resolveTenantIdForCreate(actor, command, role);
        assertTenantExistsIfPresent(tenantId);

        OffsetDateTime now = OffsetDateTime.now();
        AppUserEntity user = new AppUserEntity();
        user.setId(UUID.randomUUID());
        user.setTenantId(tenantId);
        user.setFullName(command.fullName().trim());
        user.setEmail(command.email().trim().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(command.password()));
        user.setRoleCode(role.name());
        user.setStatus("ACTIVE");
        user.setCreatedAt(now);
        user.setUpdatedAt(now);
        return appUserRepository.save(user);
    }

    @Transactional(readOnly = true)
    public List<AppUserEntity> list(String tenantIdFilter) {
        AuthenticatedUser actor = SecurityUtils.currentUser();
        assertCanManageUsers(actor);

        if (actor.isPlatformAdmin()) {
            if (tenantIdFilter != null && !tenantIdFilter.isBlank()) {
                return appUserRepository.findAllByTenantIdOrderByFullNameAsc(tenantIdFilter.trim());
            }
            return appUserRepository.findAllByTenantIdIsNotNullOrderByTenantIdAscFullNameAsc();
        }

        return appUserRepository.findAllByTenantIdOrderByFullNameAsc(requirePresidentTenant(actor));
    }

    @Transactional(readOnly = true)
    public AppUserEntity findById(UUID id) {
        AuthenticatedUser actor = SecurityUtils.currentUser();
        assertCanManageUsers(actor);

        AppUserEntity user = appUserRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        assertCanAccessUser(actor, user);
        return user;
    }

    public AppUserEntity update(UUID id, UpdateUserCommand command) {
        AppUserEntity user = findById(id);
        AuthenticatedUser actor = SecurityUtils.currentUser();
        OffsetDateTime now = OffsetDateTime.now();

        if (command.fullName() != null && !command.fullName().isBlank()) {
            user.setFullName(command.fullName().trim());
        }
        if (command.roleCode() != null && !command.roleCode().isBlank()) {
            UserRole role = UserRole.fromCode(command.roleCode());
            assertRoleAllowedOnUpdate(actor, role);
            user.setRoleCode(role.name());
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

    private void assertCanManageUsers(AuthenticatedUser actor) {
        if (actor.isPlatformAdmin() || "PRESIDENT".equalsIgnoreCase(actor.getRole())) {
            return;
        }
        throw new AccessDeniedException("Not allowed to manage users");
    }

    private void assertRoleAllowedOnCreate(AuthenticatedUser actor, UserRole role) {
        if (actor.isPlatformAdmin()) {
            if (role == UserRole.ADMIN) {
                throw new AccessDeniedException("Cannot create platform administrators via this endpoint");
            }
            if (role != UserRole.PRESIDENT && role != UserRole.TREASURY) {
                throw new AccessDeniedException("Invalid role for tenant user");
            }
            return;
        }
        if (role != UserRole.TREASURY) {
            throw new AccessDeniedException("President can only create treasury users");
        }
    }

    private void assertRoleAllowedOnUpdate(AuthenticatedUser actor, UserRole role) {
        if (actor.isPlatformAdmin()) {
            if (role == UserRole.ADMIN) {
                throw new AccessDeniedException("Cannot assign platform administrator role");
            }
            if (role != UserRole.PRESIDENT && role != UserRole.TREASURY) {
                throw new AccessDeniedException("Invalid role for tenant user");
            }
            return;
        }
        if (role != UserRole.TREASURY) {
            throw new AccessDeniedException("President can only assign treasury role");
        }
    }

    private String resolveTenantIdForCreate(AuthenticatedUser actor, CreateUserCommand command, UserRole role) {
        if (actor.isPlatformAdmin()) {
            if (role == UserRole.ADMIN) {
                return null;
            }
            if (command.tenantId() == null || command.tenantId().isBlank()) {
                throw new IllegalArgumentException("tenantId is required");
            }
            return command.tenantId().trim();
        }

        String actorTenant = requirePresidentTenant(actor);
        if (command.tenantId() != null && !command.tenantId().isBlank()
                && !actorTenant.equals(command.tenantId().trim())) {
            throw new AccessDeniedException("Cannot assign users to another tenant");
        }
        return actorTenant;
    }

    private void assertCanAccessUser(AuthenticatedUser actor, AppUserEntity user) {
        if (user.getTenantId() == null) {
            throw new AccessDeniedException("Cannot manage platform administrators");
        }
        if (actor.isPlatformAdmin()) {
            return;
        }
        if (!requirePresidentTenant(actor).equals(user.getTenantId())) {
            throw new AccessDeniedException("Cannot access users from another tenant");
        }
    }

    private String requirePresidentTenant(AuthenticatedUser actor) {
        if (actor.getTenantId() == null || actor.getTenantId().isBlank()) {
            throw new IllegalStateException("President must belong to a tenant");
        }
        return actor.getTenantId();
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
