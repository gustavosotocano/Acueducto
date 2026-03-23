package com.acueducto.veredal.waterrights.infrastructure.persistence.jpa.history;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface WaterRightOwnerHistoryRepository extends JpaRepository<WaterRightOwnerHistoryEntity, UUID> {
    Optional<WaterRightOwnerHistoryEntity> findFirstByTenantIdAndWaterRightIdAndEndDateIsNullOrderByStartDateDesc(
            String tenantId,
            UUID waterRightId
    );

    List<WaterRightOwnerHistoryEntity> findAllByTenantIdAndWaterRightIdOrderByStartDateDesc(
            String tenantId,
            UUID waterRightId
    );
}
