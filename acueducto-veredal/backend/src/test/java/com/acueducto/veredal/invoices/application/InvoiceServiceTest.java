package com.acueducto.veredal.invoices.application;

import com.acueducto.veredal.invoices.application.port.ApplicableTariffSnapshot;
import com.acueducto.veredal.invoices.application.port.ConnectionTariffLookupPort;
import com.acueducto.veredal.invoices.application.port.WaterRightBillingPort;
import com.acueducto.veredal.invoices.application.port.WaterRightBillingSnapshot;
import com.acueducto.veredal.invoices.infrastructure.persistence.jpa.InvoiceEntity;
import com.acueducto.veredal.invoices.infrastructure.persistence.jpa.InvoiceRepository;
import com.acueducto.veredal.invoices.interfaces.rest.controller.CreateInvoiceRequest;
import com.acueducto.veredal.shared.multitenancy.TenantContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InvoiceServiceTest {

    private static final String TENANT = "tenant_inv";

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private WaterRightBillingPort waterRightBillingPort;

    @Mock
    private ConnectionTariffLookupPort connectionTariffLookupPort;

    @InjectMocks
    private InvoiceService invoiceService;

    @Test
    void create_usesApplicableTariffAmount() throws Exception {
        UUID waterRightId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        WaterRightBillingSnapshot waterRight = snapshot(waterRightId, ownerId, "RESIDENTIAL", new BigDecimal("10000.00"));

        TenantContext.runWithTenant(TENANT, () -> {
            when(waterRightBillingPort.findForBilling(TENANT, waterRightId)).thenReturn(waterRight);
            when(connectionTariffLookupPort.findApplicable(TENANT, "RESIDENTIAL", LocalDate.of(2026, 4, 1)))
                    .thenReturn(Optional.of(new ApplicableTariffSnapshot(
                            new BigDecimal("25000.00"),
                            LocalDate.of(2026, 1, 1),
                            null
                    )));
            when(invoiceRepository.save(any(InvoiceEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            InvoiceEntity created = invoiceService.create(request(waterRightId, null));

            assertThat(created.getTotalAmount()).isEqualByComparingTo("25000.00");
            assertThat(created.getOwnerId()).isEqualTo(ownerId);
        });
    }

    @Test
    void create_fallsBackToBaseTariffWhenNoTariff() throws Exception {
        UUID waterRightId = UUID.randomUUID();
        WaterRightBillingSnapshot waterRight = snapshot(waterRightId, UUID.randomUUID(), "RESIDENTIAL", new BigDecimal("18000.50"));

        TenantContext.runWithTenant(TENANT, () -> {
            when(waterRightBillingPort.findForBilling(TENANT, waterRightId)).thenReturn(waterRight);
            when(connectionTariffLookupPort.findApplicable(eq(TENANT), eq("RESIDENTIAL"), any(LocalDate.class)))
                    .thenReturn(Optional.empty());
            when(invoiceRepository.save(any(InvoiceEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            InvoiceEntity created = invoiceService.create(request(waterRightId, null));

            assertThat(created.getTotalAmount()).isEqualByComparingTo("18000.50");
        });
    }

    @Test
    void create_rejectsUnknownWaterRight() {
        UUID waterRightId = UUID.randomUUID();

        assertThatThrownBy(() -> TenantContext.runWithTenant(TENANT, () -> {
            when(waterRightBillingPort.findForBilling(TENANT, waterRightId))
                    .thenThrow(new IllegalArgumentException("Water right not found"));
            invoiceService.create(request(waterRightId, new BigDecimal("1000.00")));
        })).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Water right not found");

        verify(invoiceRepository, never()).save(any());
    }

    private static WaterRightBillingSnapshot snapshot(
            UUID id,
            UUID ownerId,
            String connectionType,
            BigDecimal baseTariff
    ) {
        return new WaterRightBillingSnapshot(
                id,
                ownerId,
                connectionType,
                "ACTIVE",
                baseTariff,
                LocalDate.of(2026, 1, 1),
                null
        );
    }

    private static CreateInvoiceRequest request(UUID waterRightId, BigDecimal totalAmount) {
        return new CreateInvoiceRequest(
                waterRightId,
                "FAC-UNIT-1",
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 30),
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 30),
                totalAmount,
                null
        );
    }
}
