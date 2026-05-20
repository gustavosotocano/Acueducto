package com.acueducto.veredal.waterrights.application.transfer;

import com.acueducto.veredal.shared.multitenancy.TenantContext;
import com.acueducto.veredal.waterrights.application.port.OwnerLookupPort;
import com.acueducto.veredal.waterrights.application.port.OwnerSnapshot;
import com.acueducto.veredal.waterrights.infrastructure.persistence.jpa.WaterRightEntity;
import com.acueducto.veredal.waterrights.infrastructure.persistence.jpa.WaterRightRepository;
import com.acueducto.veredal.waterrights.infrastructure.persistence.jpa.history.WaterRightOwnerHistoryEntity;
import com.acueducto.veredal.waterrights.infrastructure.persistence.jpa.history.WaterRightOwnerHistoryRepository;
import com.acueducto.veredal.waterrights.interfaces.rest.controller.transfer.TransferWaterRightRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class WaterRightTransferService {

    private final WaterRightRepository waterRightRepository;
    private final OwnerLookupPort ownerLookupPort;
    private final WaterRightOwnerHistoryRepository waterRightOwnerHistoryRepository;

    public WaterRightTransferService(
            WaterRightRepository waterRightRepository,
            OwnerLookupPort ownerLookupPort,
            WaterRightOwnerHistoryRepository waterRightOwnerHistoryRepository
    ) {
        this.waterRightRepository = waterRightRepository;
        this.ownerLookupPort = ownerLookupPort;
        this.waterRightOwnerHistoryRepository = waterRightOwnerHistoryRepository;
    }

    public WaterRightEntity transfer(UUID waterRightId, TransferWaterRightRequest request) {
        String tenantId = TenantContext.getRequiredTenantId();
        WaterRightEntity waterRight = getWaterRight(tenantId, waterRightId);
        OwnerSnapshot newOwner = ownerLookupPort.requireForWaterRight(tenantId, request.newOwnerId());

        validateTransferRequest(waterRight, request.transferDate());
        if (waterRight.getOwnerId().equals(newOwner.id())) {
            throw new IllegalArgumentException("Water right already belongs to the provided owner");
        }

        WaterRightOwnerHistoryEntity activeHistory = waterRightOwnerHistoryRepository
                .findFirstByTenantIdAndWaterRightIdAndEndDateIsNullOrderByStartDateDesc(tenantId, waterRightId)
                .orElseThrow(() -> new IllegalStateException("Active owner history was not found for water right"));

        closeCurrentHistory(activeHistory, request.transferDate());
        createNewHistory(tenantId, waterRightId, newOwner.id(), request.transferDate(), request.transferReason(), request.notes());

        OffsetDateTime now = OffsetDateTime.now();
        waterRight.setOwnerId(newOwner.id());
        waterRight.setOwnershipStartDate(request.transferDate());
        waterRight.setOwnershipEndDate(null);
        waterRight.setUpdatedAt(now);
        return waterRightRepository.save(waterRight);
    }

    @Transactional(readOnly = true)
    public List<WaterRightOwnerHistoryEntity> findHistory(UUID waterRightId) {
        String tenantId = TenantContext.getRequiredTenantId();
        getWaterRight(tenantId, waterRightId);
        return waterRightOwnerHistoryRepository.findAllByTenantIdAndWaterRightIdOrderByStartDateDesc(tenantId, waterRightId);
    }

    public void registerInitialOwnerHistory(WaterRightEntity waterRight, String transferReason) {
        createNewHistory(
                waterRight.getTenantId(),
                waterRight.getId(),
                waterRight.getOwnerId(),
                waterRight.getOwnershipStartDate(),
                transferReason,
                "Initial owner assignment"
        );
    }

    private WaterRightEntity getWaterRight(String tenantId, UUID waterRightId) {
        return waterRightRepository.findByIdAndTenantId(waterRightId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Water right not found"));
    }

    private void validateTransferRequest(WaterRightEntity waterRight, LocalDate transferDate) {
        if (!"ACTIVE".equalsIgnoreCase(waterRight.getServiceStatus())) {
            throw new IllegalStateException("Only active water rights can be transferred");
        }
        if (transferDate.isBefore(waterRight.getOwnershipStartDate())) {
            throw new IllegalArgumentException("Transfer date cannot be before current ownership start date");
        }
    }

    private void closeCurrentHistory(WaterRightOwnerHistoryEntity activeHistory, LocalDate transferDate) {
        LocalDate historyEndDate = transferDate.minusDays(1);
        if (historyEndDate.isBefore(activeHistory.getStartDate())) {
            throw new IllegalArgumentException("Transfer date generates an invalid owner history range");
        }
        activeHistory.setEndDate(historyEndDate);
        waterRightOwnerHistoryRepository.save(activeHistory);
    }

    private void createNewHistory(
            String tenantId,
            UUID waterRightId,
            UUID ownerId,
            LocalDate startDate,
            String transferReason,
            String notes
    ) {
        WaterRightOwnerHistoryEntity history = new WaterRightOwnerHistoryEntity();
        history.setId(UUID.randomUUID());
        history.setTenantId(tenantId);
        history.setWaterRightId(waterRightId);
        history.setOwnerId(ownerId);
        history.setStartDate(startDate);
        history.setEndDate(null);
        history.setTransferReason(transferReason);
        history.setNotes(notes);
        history.setCreatedAt(OffsetDateTime.now());
        waterRightOwnerHistoryRepository.save(history);
    }
}
