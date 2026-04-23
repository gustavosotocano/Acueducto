package com.acueducto.veredal.interests.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InterestTierRepository extends JpaRepository<InterestTierEntity, UUID> {
    List<InterestTierEntity> findAllByTenantIdOrderByDayFromAsc(String tenantId);
}

