package com.acueducto.veredal.properties.application.ownership;

import com.acueducto.veredal.owners.infrastructure.persistence.jpa.OwnerEntity;
import com.acueducto.veredal.owners.infrastructure.persistence.jpa.OwnerRepository;
import com.acueducto.veredal.properties.infrastructure.persistence.jpa.PropertyEntity;
import com.acueducto.veredal.properties.infrastructure.persistence.jpa.PropertyRepository;
import com.acueducto.veredal.properties.infrastructure.persistence.jpa.history.PropertyOwnerHistoryEntity;
import com.acueducto.veredal.properties.infrastructure.persistence.jpa.history.PropertyOwnerHistoryRepository;
import com.acueducto.veredal.properties.interfaces.rest.controller.ownership.ChangePropertyOwnerRequest;
import com.acueducto.veredal.shared.multitenancy.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class PropertyOwnershipService {

    private final PropertyRepository propertyRepository;
    private final OwnerRepository ownerRepository;
    private final PropertyOwnerHistoryRepository propertyOwnerHistoryRepository;

    public PropertyOwnershipService(
            PropertyRepository propertyRepository,
            OwnerRepository ownerRepository,
            PropertyOwnerHistoryRepository propertyOwnerHistoryRepository
    ) {
        this.propertyRepository = propertyRepository;
        this.ownerRepository = ownerRepository;
        this.propertyOwnerHistoryRepository = propertyOwnerHistoryRepository;
    }

    public PropertyEntity changeOwner(UUID propertyId, ChangePropertyOwnerRequest request) {
        String tenantId = TenantContext.getRequiredTenantId();
        PropertyEntity property = getProperty(tenantId, propertyId);
        OwnerEntity newOwner = getOwner(tenantId, request.newOwnerId());

        if (property.getOwnerId().equals(newOwner.getId())) {
            throw new IllegalArgumentException("Property already belongs to the provided owner");
        }

        PropertyOwnerHistoryEntity activeHistory = propertyOwnerHistoryRepository
                .findFirstByTenantIdAndPropertyIdAndEndDateIsNullOrderByStartDateDesc(tenantId, propertyId)
                .orElseThrow(() -> new IllegalStateException("Active owner history was not found for property"));

        validateChangeDate(activeHistory, request.changeDate());
        closeCurrentHistory(activeHistory, request.changeDate());
        createNewHistory(tenantId, propertyId, newOwner.getId(), request.changeDate(), request.changeReason(), request.notes());

        property.setOwnerId(newOwner.getId());
        property.setUpdatedAt(OffsetDateTime.now());
        return propertyRepository.save(property);
    }

    @Transactional(readOnly = true)
    public List<PropertyOwnerHistoryEntity> findHistory(UUID propertyId) {
        String tenantId = TenantContext.getRequiredTenantId();
        getProperty(tenantId, propertyId);
        return propertyOwnerHistoryRepository.findAllByTenantIdAndPropertyIdOrderByStartDateDesc(tenantId, propertyId);
    }

    public void registerInitialOwnerHistory(PropertyEntity property, LocalDate startDate, String changeReason) {
        createNewHistory(
                property.getTenantId(),
                property.getId(),
                property.getOwnerId(),
                startDate,
                changeReason,
                "Initial property owner assignment"
        );
    }

    private PropertyEntity getProperty(String tenantId, UUID propertyId) {
        return propertyRepository.findByIdAndTenantId(propertyId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Property not found"));
    }

    private OwnerEntity getOwner(String tenantId, UUID ownerId) {
        return ownerRepository.findByIdAndTenantId(ownerId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Owner does not exist in this tenant"));
    }

    private void validateChangeDate(PropertyOwnerHistoryEntity activeHistory, LocalDate changeDate) {
        if (changeDate.isBefore(activeHistory.getStartDate())) {
            throw new IllegalArgumentException("Change date cannot be before current property ownership start date");
        }
    }

    private void closeCurrentHistory(PropertyOwnerHistoryEntity activeHistory, LocalDate changeDate) {
        LocalDate endDate = changeDate.minusDays(1);
        if (endDate.isBefore(activeHistory.getStartDate())) {
            throw new IllegalArgumentException("Change date generates an invalid property owner history range");
        }
        activeHistory.setEndDate(endDate);
        propertyOwnerHistoryRepository.save(activeHistory);
    }

    private void createNewHistory(
            String tenantId,
            UUID propertyId,
            UUID ownerId,
            LocalDate startDate,
            String changeReason,
            String notes
    ) {
        PropertyOwnerHistoryEntity history = new PropertyOwnerHistoryEntity();
        history.setId(UUID.randomUUID());
        history.setTenantId(tenantId);
        history.setPropertyId(propertyId);
        history.setOwnerId(ownerId);
        history.setStartDate(startDate);
        history.setEndDate(null);
        history.setChangeReason(changeReason);
        history.setNotes(notes);
        history.setCreatedAt(OffsetDateTime.now());
        propertyOwnerHistoryRepository.save(history);
    }
}
