package com.acueducto.veredal.waterrights.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WaterRightRepository extends JpaRepository<WaterRightEntity, UUID> {
    List<WaterRightEntity> findAllByTenantIdOrderByRightCodeAsc(String tenantId);

    List<WaterRightEntity> findAllByTenantIdAndServiceStatusOrderByRightCodeAsc(String tenantId, String serviceStatus);

    Optional<WaterRightEntity> findByIdAndTenantId(UUID id, String tenantId);
    boolean existsByTenantIdAndRightCode(String tenantId, String rightCode);
}
