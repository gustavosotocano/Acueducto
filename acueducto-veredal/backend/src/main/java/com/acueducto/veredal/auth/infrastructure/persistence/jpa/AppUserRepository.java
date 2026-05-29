package com.acueducto.veredal.auth.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AppUserRepository extends JpaRepository<AppUserEntity, UUID> {

    Optional<AppUserEntity> findByEmailIgnoreCase(String email);

    Optional<AppUserEntity> findByIdAndTenantId(UUID id, String tenantId);

    List<AppUserEntity> findAllByTenantIdOrderByFullNameAsc(String tenantId);

    List<AppUserEntity> findAllByTenantIdIsNotNullOrderByTenantIdAscFullNameAsc();

    boolean existsByEmailIgnoreCase(String email);
}
