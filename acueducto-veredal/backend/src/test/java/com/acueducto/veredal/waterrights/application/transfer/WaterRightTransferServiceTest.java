package com.acueducto.veredal.waterrights.application.transfer;

import com.acueducto.veredal.shared.multitenancy.TenantContext;
import com.acueducto.veredal.waterrights.application.port.OwnerLookupPort;
import com.acueducto.veredal.waterrights.application.port.OwnerSnapshot;
import com.acueducto.veredal.waterrights.domain.ConnectionType;
import com.acueducto.veredal.waterrights.infrastructure.persistence.jpa.WaterRightEntity;
import com.acueducto.veredal.waterrights.infrastructure.persistence.jpa.WaterRightRepository;
import com.acueducto.veredal.waterrights.infrastructure.persistence.jpa.history.WaterRightOwnerHistoryEntity;
import com.acueducto.veredal.waterrights.infrastructure.persistence.jpa.history.WaterRightOwnerHistoryRepository;
import com.acueducto.veredal.waterrights.interfaces.rest.controller.transfer.TransferWaterRightRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WaterRightTransferServiceTest {

    private static final String TENANT = "tenant_xfer";

    @Mock
    private WaterRightRepository waterRightRepository;

    @Mock
    private OwnerLookupPort ownerLookupPort;

    @Mock
    private WaterRightOwnerHistoryRepository waterRightOwnerHistoryRepository;

    @InjectMocks
    private WaterRightTransferService waterRightTransferService;

    @Test
    void transfer_rejectsSameOwner() {
        UUID waterRightId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        WaterRightEntity waterRight = activeWaterRight(waterRightId, ownerId);

        assertThatThrownBy(() -> TenantContext.runWithTenant(TENANT, () -> {
            when(waterRightRepository.findByIdAndTenantId(waterRightId, TENANT)).thenReturn(Optional.of(waterRight));
            when(ownerLookupPort.requireForWaterRight(TENANT, ownerId)).thenReturn(new OwnerSnapshot(ownerId, "ACTIVE"));
            waterRightTransferService.transfer(waterRightId, transferRequest(ownerId, LocalDate.of(2026, 3, 1)));
        })).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Water right already belongs to the provided owner");
    }

    @Test
    void transfer_updatesOwnerAndHistory() throws Exception {
        UUID waterRightId = UUID.randomUUID();
        UUID currentOwnerId = UUID.randomUUID();
        UUID newOwnerId = UUID.randomUUID();
        WaterRightEntity waterRight = activeWaterRight(waterRightId, currentOwnerId);

        WaterRightOwnerHistoryEntity activeHistory = new WaterRightOwnerHistoryEntity();
        activeHistory.setId(UUID.randomUUID());
        activeHistory.setTenantId(TENANT);
        activeHistory.setWaterRightId(waterRightId);
        activeHistory.setOwnerId(currentOwnerId);
        activeHistory.setStartDate(LocalDate.of(2026, 1, 1));

        TenantContext.runWithTenant(TENANT, () -> {
            when(waterRightRepository.findByIdAndTenantId(waterRightId, TENANT)).thenReturn(Optional.of(waterRight));
            when(ownerLookupPort.requireForWaterRight(TENANT, newOwnerId)).thenReturn(new OwnerSnapshot(newOwnerId, "ACTIVE"));
            when(waterRightOwnerHistoryRepository.findFirstByTenantIdAndWaterRightIdAndEndDateIsNullOrderByStartDateDesc(
                    TENANT, waterRightId)).thenReturn(Optional.of(activeHistory));
            when(waterRightRepository.save(any(WaterRightEntity.class))).thenAnswer(inv -> inv.getArgument(0));
            when(waterRightOwnerHistoryRepository.save(any(WaterRightOwnerHistoryEntity.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            LocalDate transferDate = LocalDate.of(2026, 3, 10);
            WaterRightEntity result = waterRightTransferService.transfer(waterRightId, transferRequest(newOwnerId, transferDate));

            assertThat(result.getOwnerId()).isEqualTo(newOwnerId);
            assertThat(result.getOwnershipStartDate()).isEqualTo(transferDate);
            verify(waterRightOwnerHistoryRepository).save(activeHistory);
        });
    }

    private static WaterRightEntity activeWaterRight(UUID id, UUID ownerId) {
        WaterRightEntity entity = new WaterRightEntity();
        entity.setId(id);
        entity.setTenantId(TENANT);
        entity.setRightCode("WR-X");
        entity.setOwnerId(ownerId);
        entity.setPropertyId(UUID.randomUUID());
        entity.setServiceStatus("ACTIVE");
        entity.setConnectionType(ConnectionType.RESIDENTIAL);
        entity.setBaseTariff(new BigDecimal("1000"));
        entity.setOwnershipStartDate(LocalDate.of(2026, 1, 1));
        entity.setCreatedAt(OffsetDateTime.now());
        entity.setUpdatedAt(OffsetDateTime.now());
        return entity;
    }

    private static TransferWaterRightRequest transferRequest(UUID newOwnerId, LocalDate transferDate) {
        return new TransferWaterRightRequest(newOwnerId, transferDate, "SALE", "notes");
    }
}
