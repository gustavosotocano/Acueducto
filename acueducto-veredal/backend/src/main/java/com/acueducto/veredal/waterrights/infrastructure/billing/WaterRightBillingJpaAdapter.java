package com.acueducto.veredal.waterrights.infrastructure.billing;

import com.acueducto.veredal.invoices.application.port.WaterRightBillingPort;
import com.acueducto.veredal.invoices.application.port.WaterRightBillingSnapshot;
import com.acueducto.veredal.waterrights.infrastructure.persistence.jpa.WaterRightEntity;
import com.acueducto.veredal.waterrights.infrastructure.persistence.jpa.WaterRightRepository;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

@Component
public class WaterRightBillingJpaAdapter implements WaterRightBillingPort {

    private final WaterRightRepository waterRightRepository;

    public WaterRightBillingJpaAdapter(WaterRightRepository waterRightRepository) {
        this.waterRightRepository = waterRightRepository;
    }

    @Override
    public WaterRightBillingSnapshot findForBilling(String tenantId, UUID waterRightId) {
        WaterRightEntity entity = waterRightRepository.findByIdAndTenantId(waterRightId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Water right not found"));
        return toSnapshot(entity);
    }

    @Override
    public List<WaterRightBillingSnapshot> findActiveForMonthlyBatch(String tenantId) {
        return waterRightRepository.findAllByTenantIdAndServiceStatusOrderByRightCodeAsc(tenantId, "ACTIVE")
                .stream()
                .map(WaterRightBillingJpaAdapter::toSnapshot)
                .toList();
    }

    private static WaterRightBillingSnapshot toSnapshot(WaterRightEntity entity) {
        return new WaterRightBillingSnapshot(
                entity.getId(),
                entity.getOwnerId(),
                entity.getConnectionType().name(),
                entity.getServiceStatus(),
                entity.getBaseTariff(),
                entity.getOwnershipStartDate(),
                entity.getOwnershipEndDate()
        );
    }
}
