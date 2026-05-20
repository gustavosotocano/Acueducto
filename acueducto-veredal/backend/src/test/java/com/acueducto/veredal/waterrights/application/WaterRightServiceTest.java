package com.acueducto.veredal.waterrights.application;

import com.acueducto.veredal.shared.multitenancy.TenantContext;
import com.acueducto.veredal.waterrights.application.port.OwnerLookupPort;
import com.acueducto.veredal.waterrights.application.port.PropertyLookupPort;
import com.acueducto.veredal.waterrights.application.transfer.WaterRightTransferService;
import com.acueducto.veredal.waterrights.domain.ConnectionType;
import com.acueducto.veredal.waterrights.infrastructure.persistence.jpa.WaterRightRepository;
import com.acueducto.veredal.waterrights.interfaces.rest.controller.CreateWaterRightRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WaterRightServiceTest {

    private static final String TENANT = "tenant_wr";

    @Mock
    private WaterRightRepository waterRightRepository;

    @Mock
    private OwnerLookupPort ownerLookupPort;

    @Mock
    private PropertyLookupPort propertyLookupPort;

    @Mock
    private WaterRightTransferService waterRightTransferService;

    @InjectMocks
    private WaterRightService waterRightService;

    @Test
    void create_rejectsUnknownOwner() {
        UUID ownerId = UUID.randomUUID();

        assertThatThrownBy(() -> TenantContext.runWithTenant(TENANT, () -> {
            when(waterRightRepository.existsByTenantIdAndRightCode(TENANT, "WR-1")).thenReturn(false);
            when(ownerLookupPort.requireForWaterRight(TENANT, ownerId))
                    .thenThrow(new IllegalArgumentException("Owner does not exist in this tenant"));
            waterRightService.create(createRequest(ownerId, UUID.randomUUID()));
        })).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Owner does not exist in this tenant");

        verify(waterRightRepository, never()).save(any());
    }

    @Test
    void create_rejectsUnknownProperty() {
        UUID ownerId = UUID.randomUUID();
        UUID propertyId = UUID.randomUUID();

        assertThatThrownBy(() -> TenantContext.runWithTenant(TENANT, () -> {
            when(waterRightRepository.existsByTenantIdAndRightCode(TENANT, "WR-1")).thenReturn(false);
            when(ownerLookupPort.requireForWaterRight(TENANT, ownerId))
                    .thenReturn(new com.acueducto.veredal.waterrights.application.port.OwnerSnapshot(ownerId, "ACTIVE"));
            when(propertyLookupPort.requireForWaterRight(TENANT, propertyId))
                    .thenThrow(new IllegalArgumentException("Property does not exist in this tenant"));
            waterRightService.create(createRequest(ownerId, propertyId));
        })).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Property does not exist in this tenant");

        verify(waterRightRepository, never()).save(any());
    }

    private static CreateWaterRightRequest createRequest(UUID ownerId, UUID propertyId) {
        return new CreateWaterRightRequest(
                "WR-1",
                ownerId,
                propertyId,
                null,
                "ACTIVE",
                ConnectionType.RESIDENTIAL,
                new BigDecimal("25000"),
                LocalDate.of(2026, 1, 1)
        );
    }
}
