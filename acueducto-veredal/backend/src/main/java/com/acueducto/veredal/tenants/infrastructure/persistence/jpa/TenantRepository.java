package com.acueducto.veredal.tenants.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TenantRepository extends JpaRepository<TenantEntity, String> {
    Optional<TenantEntity> findByIdAndStatus(String id, String status);
}

