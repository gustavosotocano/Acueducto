package com.acueducto.veredal.properties.application;

import com.acueducto.veredal.owners.infrastructure.persistence.jpa.OwnerEntity;
import com.acueducto.veredal.owners.infrastructure.persistence.jpa.OwnerRepository;
import com.acueducto.veredal.properties.application.ownership.PropertyOwnershipService;
import com.acueducto.veredal.properties.infrastructure.persistence.jpa.PropertyEntity;
import com.acueducto.veredal.properties.infrastructure.persistence.jpa.PropertyRepository;
import com.acueducto.veredal.properties.interfaces.rest.controller.CreatePropertyRequest;
import com.acueducto.veredal.properties.interfaces.rest.controller.UpdatePropertyRequest;
import com.acueducto.veredal.shared.multitenancy.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class PropertyService {

    private final PropertyRepository propertyRepository;
    private final OwnerRepository ownerRepository;
    private final PropertyOwnershipService propertyOwnershipService;

    public PropertyService(
            PropertyRepository propertyRepository,
            OwnerRepository ownerRepository,
            PropertyOwnershipService propertyOwnershipService
    ) {
        this.propertyRepository = propertyRepository;
        this.ownerRepository = ownerRepository;
        this.propertyOwnershipService = propertyOwnershipService;
    }

    public PropertyEntity create(CreatePropertyRequest request) {
        String tenantId = TenantContext.getRequiredTenantId();
        if (propertyRepository.existsByTenantIdAndPropertyCode(tenantId, request.propertyCode())) {
            throw new IllegalArgumentException("Property code already exists in this tenant");
        }

        OwnerEntity owner = ownerRepository.findByIdAndTenantId(request.ownerId(), tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Owner does not exist in this tenant"));

        OffsetDateTime now = OffsetDateTime.now();
        PropertyEntity entity = new PropertyEntity();
        entity.setId(UUID.randomUUID());
        entity.setTenantId(tenantId);
        entity.setPropertyCode(request.propertyCode());
        entity.setPropertyName(request.propertyName());
        entity.setAddressLine(request.addressLine());
        entity.setOwnerId(owner.getId());
        entity.setStatus("ACTIVE");
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        PropertyEntity saved = propertyRepository.save(entity);
        propertyOwnershipService.registerInitialOwnerHistory(saved, request.ownershipStartDate(), "INITIAL_ASSIGNMENT");
        return saved;
    }

    @Transactional(readOnly = true)
    public List<PropertyEntity> findAll() {
        return propertyRepository.findAllByTenantIdOrderByCreatedAtDesc(TenantContext.getRequiredTenantId());
    }

    @Transactional(readOnly = true)
    public PropertyEntity findById(UUID id) {
        return propertyRepository.findByIdAndTenantId(id, TenantContext.getRequiredTenantId())
                .orElseThrow(() -> new IllegalArgumentException("Property not found"));
    }

    public PropertyEntity update(UUID id, UpdatePropertyRequest request) {
        PropertyEntity entity = findById(id);
        entity.setPropertyName(request.propertyName());
        entity.setAddressLine(request.addressLine());
        entity.setUpdatedAt(OffsetDateTime.now());
        return propertyRepository.save(entity);
    }

    public PropertyEntity deactivate(UUID id) {
        PropertyEntity entity = findById(id);
        entity.setStatus("INACTIVE");
        entity.setUpdatedAt(OffsetDateTime.now());
        return propertyRepository.save(entity);
    }
}
