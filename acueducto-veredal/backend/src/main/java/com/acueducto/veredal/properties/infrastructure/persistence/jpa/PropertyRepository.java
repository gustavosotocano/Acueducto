package com.acueducto.veredal.properties.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PropertyRepository extends JpaRepository<PropertyEntity, UUID> {

    List<PropertyEntity> findAllByTenantIdOrderByCreatedAtDesc(String tenantId);

    Optional<PropertyEntity> findByIdAndTenantId(UUID id, String tenantId);

    boolean existsByTenantIdAndPropertyCode(String tenantId, String propertyCode);
}
