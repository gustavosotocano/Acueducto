package com.acueducto.veredal.properties.infrastructure.persistence.jpa.history;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PropertyOwnerHistoryRepository extends JpaRepository<PropertyOwnerHistoryEntity, UUID> {

    Optional<PropertyOwnerHistoryEntity> findFirstByTenantIdAndPropertyIdAndEndDateIsNullOrderByStartDateDesc(
            String tenantId,
            UUID propertyId
    );

    List<PropertyOwnerHistoryEntity> findAllByTenantIdAndPropertyIdOrderByStartDateDesc(String tenantId, UUID propertyId);
}
