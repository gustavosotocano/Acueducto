package com.acueducto.veredal.auth.application;

import com.acueducto.veredal.auth.infrastructure.persistence.jpa.AppUserEntity;
import com.acueducto.veredal.auth.infrastructure.persistence.jpa.AppUserRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.UUID;

@Component
@Profile({"local", "test"})
public class AuthDataInitializer implements ApplicationRunner {

    public static final String PLATFORM_ADMIN_EMAIL = "admin@acueducto.local";
    public static final String DEFAULT_PASSWORD = "ChangeMe123!";

    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthDataInitializer(AppUserRepository appUserRepository, PasswordEncoder passwordEncoder) {
        this.appUserRepository = appUserRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (appUserRepository.existsByEmailIgnoreCase(PLATFORM_ADMIN_EMAIL)) {
            return;
        }

        OffsetDateTime now = OffsetDateTime.now();
        AppUserEntity admin = new AppUserEntity();
        admin.setId(UUID.randomUUID());
        admin.setTenantId(null);
        admin.setFullName("Administrador plataforma");
        admin.setEmail(PLATFORM_ADMIN_EMAIL);
        admin.setPasswordHash(passwordEncoder.encode(DEFAULT_PASSWORD));
        admin.setRoleCode("ADMIN");
        admin.setStatus("ACTIVE");
        admin.setCreatedAt(now);
        admin.setUpdatedAt(now);
        appUserRepository.save(admin);
    }
}
