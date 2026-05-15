package com.acueducto.veredal.auth.interfaces.rest.controller;

import com.acueducto.veredal.auth.application.UserService;
import com.acueducto.veredal.auth.infrastructure.persistence.jpa.AppUserEntity;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse create(@Valid @RequestBody CreateUserRequest request) {
        return toResponse(userService.create(new UserService.CreateUserCommand(
                request.fullName(),
                request.email(),
                request.password(),
                request.roleCode(),
                request.tenantId()
        )));
    }

    @GetMapping
    public List<UserResponse> list() {
        return userService.listForCurrentTenant().stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public UserResponse findById(@PathVariable UUID id) {
        return toResponse(userService.findById(id));
    }

    @PutMapping("/{id}")
    public UserResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateUserRequest request) {
        return toResponse(userService.update(id, new UserService.UpdateUserCommand(
                request.fullName(),
                request.roleCode(),
                request.status(),
                request.password()
        )));
    }

    @PostMapping("/{id}/reset-password")
    public UserResponse resetPassword(@PathVariable UUID id, @Valid @RequestBody AdminResetPasswordRequest request) {
        return toResponse(userService.adminResetPassword(id, request.newPassword()));
    }

    private UserResponse toResponse(AppUserEntity entity) {
        return new UserResponse(
                entity.getId(),
                entity.getTenantId(),
                entity.getFullName(),
                entity.getEmail(),
                entity.getRoleCode(),
                entity.getStatus(),
                entity.getLastLoginAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
