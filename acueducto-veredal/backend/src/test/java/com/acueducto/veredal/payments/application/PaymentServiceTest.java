package com.acueducto.veredal.payments.application;

import com.acueducto.veredal.payments.application.port.InvoiceForPayment;
import com.acueducto.veredal.payments.application.port.InvoicePaymentPort;
import com.acueducto.veredal.payments.infrastructure.persistence.jpa.PaymentEntity;
import com.acueducto.veredal.payments.infrastructure.persistence.jpa.PaymentRepository;
import com.acueducto.veredal.payments.interfaces.rest.controller.CreatePaymentRequest;
import com.acueducto.veredal.shared.multitenancy.TenantContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    private static final String TENANT = "tenant_unit";

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private InvoicePaymentPort invoicePaymentPort;

    @InjectMocks
    private PaymentService paymentService;

    @AfterEach
    void clearTenant() {
        // no-op: tests use runWithTenant per case
    }

    @Test
    void create_marksInvoicePaidWhenPaymentCoversTotal() throws Exception {
        UUID invoiceId = UUID.randomUUID();
        InvoiceForPayment invoice = new InvoiceForPayment(
                invoiceId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("25000.00"),
                "ISSUED"
        );

        TenantContext.runWithTenant(TENANT, () -> {
            when(invoicePaymentPort.findForPayment(TENANT, invoiceId)).thenReturn(invoice);
            when(paymentRepository.sumAmountByTenantIdAndInvoiceId(TENANT, invoiceId))
                    .thenReturn(BigDecimal.ZERO)
                    .thenReturn(new BigDecimal("25000.00"));
            when(paymentRepository.save(any(PaymentEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            CreatePaymentRequest request = new CreatePaymentRequest(
                    invoiceId,
                    LocalDate.of(2026, 4, 28),
                    new BigDecimal("25000.00"),
                    "CASH",
                    null,
                    null
            );

            PaymentEntity saved = paymentService.create(request);

            assertThat(saved.getInvoiceId()).isEqualTo(invoiceId);
            assertThat(saved.getTenantId()).isEqualTo(TENANT);
            verify(invoicePaymentPort).markPaidIfFullyCovered(TENANT, invoiceId, new BigDecimal("25000.00"));
        });
    }

    @Test
    void create_doesNotMarkPaidOnPartialPayment() throws Exception {
        UUID invoiceId = UUID.randomUUID();
        InvoiceForPayment invoice = new InvoiceForPayment(
                invoiceId,
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("25000.00"),
                "ISSUED"
        );

        TenantContext.runWithTenant(TENANT, () -> {
            when(invoicePaymentPort.findForPayment(TENANT, invoiceId)).thenReturn(invoice);
            when(paymentRepository.sumAmountByTenantIdAndInvoiceId(TENANT, invoiceId))
                    .thenReturn(BigDecimal.ZERO)
                    .thenReturn(new BigDecimal("10000.00"));
            when(paymentRepository.save(any(PaymentEntity.class))).thenAnswer(inv -> inv.getArgument(0));

            CreatePaymentRequest request = new CreatePaymentRequest(
                    invoiceId,
                    LocalDate.of(2026, 4, 28),
                    new BigDecimal("10000.00"),
                    "CASH",
                    null,
                    null
            );

            paymentService.create(request);

            verify(invoicePaymentPort).markPaidIfFullyCovered(TENANT, invoiceId, new BigDecimal("10000.00"));
        });
    }

    @Test
    void create_rejectsUnknownInvoice() {
        UUID invoiceId = UUID.randomUUID();

        assertThatThrownBy(() -> TenantContext.runWithTenant(TENANT, () -> {
            when(invoicePaymentPort.findForPayment(TENANT, invoiceId))
                    .thenThrow(new IllegalArgumentException("Invoice not found"));
            paymentService.create(new CreatePaymentRequest(
                    invoiceId,
                    LocalDate.of(2026, 4, 28),
                    new BigDecimal("1000.00"),
                    "CASH",
                    null,
                    null
            ));
        })).isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Invoice not found");

        verify(paymentRepository, never()).save(any());
    }
}
