package com.acueducto.veredal.auth.interfaces.rest.controller;

import com.acueducto.veredal.auth.application.AuthService;
import com.acueducto.veredal.shared.security.AuthenticatedUser;
import com.acueducto.veredal.shared.security.SecurityUtils;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        AuthService.LoginResult result = authService.login(request.email(), request.password());
        return new LoginResponse(result.accessToken(), toResponse(result.user()));
    }

    @GetMapping("/me")
    public AuthUserResponse me() {
        AuthenticatedUser current = SecurityUtils.currentUser();
        AuthService.UserProfile profile = authService.me(current.getId());
        return toResponse(profile);
    }

    @PostMapping("/password-reset-request")
    public AuthService.PasswordResetRequestResult requestPasswordReset(
            @Valid @RequestBody PasswordResetRequest request
    ) {
        return authService.requestPasswordReset(request.email());
    }

    @PostMapping("/password-reset")
    public AuthService.PasswordResetResult resetPassword(@Valid @RequestBody PasswordResetConfirmRequest request) {
        return authService.resetPassword(request.token(), request.newPassword());
    }

    private AuthUserResponse toResponse(AuthService.UserProfile profile) {
        return new AuthUserResponse(
                profile.id(),
                profile.tenantId(),
                profile.fullName(),
                profile.email(),
                profile.role(),
                profile.status()
        );
    }
}
