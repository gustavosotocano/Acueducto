package com.acueducto.veredal.waterrights.application;

import com.acueducto.veredal.owners.infrastructure.persistence.jpa.OwnerRepository;
import com.acueducto.veredal.properties.infrastructure.persistence.jpa.PropertyRepository;
import com.acueducto.veredal.shared.multitenancy.TenantContext;
import com.acueducto.veredal.waterrights.application.transfer.WaterRightTransferService;
import com.acueducto.veredal.waterrights.infrastructure.persistence.jpa.WaterRightEntity;
import com.acueducto.veredal.waterrights.infrastructure.persistence.jpa.WaterRightRepository;
import com.acueducto.veredal.waterrights.interfaces.rest.controller.CreateWaterRightRequest;
import com.acueducto.veredal.waterrights.interfaces.rest.controller.DeactivateWaterRightRequest;
import com.acueducto.veredal.waterrights.interfaces.rest.controller.UpdateWaterRightRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class WaterRightService {

    private final WaterRightRepository waterRightRepository;
    private final OwnerRepository ownerRepository;
    private final PropertyRepository propertyRepository;
    private final WaterRightTransferService waterRightTransferService;

    public WaterRightService(
            WaterRightRepository waterRightRepository,
            OwnerRepository ownerRepository,
            PropertyRepository propertyRepository,
            WaterRightTransferService waterRightTransferService
    ) {
        this.waterRightRepository = waterRightRepository;
        this.ownerRepository = ownerRepository;
        this.propertyRepository = propertyRepository;
        this.waterRightTransferService = waterRightTransferService;
    }

    public WaterRightEntity create(CreateWaterRightRequest request) {
        String tenantId = TenantContext.getRequiredTenantId();
        if (waterRightRepository.existsByTenantIdAndRightCode(tenantId, request.rightCode())) {
            throw new IllegalArgumentException("Water right code already exists in this tenant");
        }
        if (ownerRepository.findByIdAndTenantId(request.ownerId(), tenantId).isEmpty()) {
            throw new IllegalArgumentException("Owner does not exist in this tenant");
        }
        if (propertyRepository.findByIdAndTenantId(request.propertyId(), tenantId).isEmpty()) {
            throw new IllegalArgumentException("Property does not exist in this tenant");
        }

        OffsetDateTime now = OffsetDateTime.now();
        WaterRightEntity entity = new WaterRightEntity();
        entity.setId(UUID.randomUUID());
        entity.setTenantId(tenantId);
        entity.setRightCode(request.rightCode());
        entity.setOwnerId(request.ownerId());
        entity.setPropertyId(request.propertyId());
        entity.setWaterMeterId(request.waterMeterId());
        entity.setServiceStatus(request.serviceStatus());
        entity.setConnectionType(request.connectionType());
        entity.setBaseTariff(request.baseTariff());
        entity.setOwnershipStartDate(request.ownershipStartDate());
        entity.setOwnershipEndDate(null);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        WaterRightEntity saved = waterRightRepository.save(entity);
        waterRightTransferService.registerInitialOwnerHistory(saved, "INITIAL_ASSIGNMENT");
        return saved;
    }

    @Transactional(readOnly = true)
    public List<WaterRightEntity> findAll() {
        return waterRightRepository.findAllByTenantIdOrderByRightCodeAsc(TenantContext.getRequiredTenantId());
    }

    @Transactional(readOnly = true)
    public WaterRightEntity findById(UUID id) {
        return waterRightRepository.findByIdAndTenantId(id, TenantContext.getRequiredTenantId())
                .orElseThrow(() -> new IllegalArgumentException("Water right not found"));
    }

    public WaterRightEntity update(UUID id, UpdateWaterRightRequest request) {
        String tenantId = TenantContext.getRequiredTenantId();
        if (propertyRepository.findByIdAndTenantId(request.propertyId(), tenantId).isEmpty()) {
            throw new IllegalArgumentException("Property does not exist in this tenant");
        }

        WaterRightEntity entity = findById(id);
        entity.setPropertyId(request.propertyId());
        entity.setWaterMeterId(request.waterMeterId());
        entity.setServiceStatus(request.serviceStatus());
        entity.setConnectionType(request.connectionType());
        entity.setBaseTariff(request.baseTariff());
        entity.setUpdatedAt(OffsetDateTime.now());
        return waterRightRepository.save(entity);
    }

    public WaterRightEntity deactivate(UUID id, DeactivateWaterRightRequest request) {
        WaterRightEntity entity = findById(id);
        if (request.ownershipEndDate().isBefore(entity.getOwnershipStartDate())) {
            throw new IllegalArgumentException("Ownership end date cannot be before ownership start date");
        }

        entity.setServiceStatus("INACTIVE");
        entity.setOwnershipEndDate(request.ownershipEndDate());
        entity.setUpdatedAt(OffsetDateTime.now());
        return waterRightRepository.save(entity);
    }
}
