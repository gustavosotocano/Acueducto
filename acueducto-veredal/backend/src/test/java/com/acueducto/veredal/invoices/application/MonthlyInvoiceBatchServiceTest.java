package com.acueducto.veredal.invoices.application;

import com.acueducto.veredal.invoices.application.port.WaterRightBillingPort;
import com.acueducto.veredal.invoices.application.port.WaterRightBillingSnapshot;
import com.acueducto.veredal.invoices.infrastructure.persistence.jpa.InvoiceEntity;
import com.acueducto.veredal.invoices.infrastructure.persistence.jpa.InvoiceRepository;
import com.acueducto.veredal.invoices.interfaces.rest.controller.BatchMonthlyInvoiceRequest;
import com.acueducto.veredal.shared.multitenancy.TenantContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MonthlyInvoiceBatchServiceTest {

    private static final String TENANT = "tenant_batch";

    @Mock
    private WaterRightBillingPort waterRightBillingPort;

    @Mock
    private InvoiceRepository invoiceRepository;

    @Mock
    private InvoiceService invoiceService;

    @Mock
    private InvoiceNumberAllocator invoiceNumberAllocator;

    @InjectMocks
    private MonthlyInvoiceBatchService monthlyInvoiceBatchService;

    @Test
    void run_skipsWaterRightWhenOwnershipNotActiveForPeriod() throws Exception {
        UUID activeId = UUID.randomUUID();
        UUID inactiveId = UUID.randomUUID();

        WaterRightBillingSnapshot active = new WaterRightBillingSnapshot(
                activeId,
                UUID.randomUUID(),
                "RESIDENTIAL",
                "ACTIVE",
                new BigDecimal("25000"),
                LocalDate.of(2026, 1, 1),
                null
        );
        WaterRightBillingSnapshot inactive = new WaterRightBillingSnapshot(
                inactiveId,
                UUID.randomUUID(),
                "RESIDENTIAL",
                "ACTIVE",
                new BigDecimal("25000"),
                LocalDate.of(2026, 6, 1),
                null
        );

        TenantContext.runWithTenant(TENANT, () -> {
            when(waterRightBillingPort.findActiveForMonthlyBatch(TENANT)).thenReturn(List.of(active, inactive));
            when(invoiceRepository.existsByTenantIdAndWaterRightIdAndPeriodStartAndPeriodEndAndStatusNot(
                    eq(TENANT), any(), any(), any(), eq("CANCELLED"))).thenReturn(false);
            when(invoiceNumberAllocator.allocateNext(TENANT)).thenReturn("FAC-BATCH-1");

            InvoiceEntity created = new InvoiceEntity();
            created.setId(UUID.randomUUID());
            when(invoiceService.createForBatch(
                    eq(activeId),
                    eq("FAC-BATCH-1"),
                    any(),
                    any(),
                    any(),
                    any(),
                    any()
            )).thenReturn(created);

            var response = monthlyInvoiceBatchService.run(new BatchMonthlyInvoiceRequest(2026, 4, null, null));

            assertThat(response.createdCount()).isEqualTo(1);
            assertThat(response.skippedCount()).isEqualTo(1);
            assertThat(response.skipped().getFirst().waterRightId()).isEqualTo(inactiveId);
            assertThat(response.skipped().getFirst().reason()).isEqualTo("OWNERSHIP_NOT_ACTIVE_FOR_PERIOD");
            verify(invoiceService, never()).createForBatch(
                    eq(inactiveId),
                    any(),
                    any(),
                    any(),
                    any(),
                    any(),
                    any()
            );
        });
    }
}
