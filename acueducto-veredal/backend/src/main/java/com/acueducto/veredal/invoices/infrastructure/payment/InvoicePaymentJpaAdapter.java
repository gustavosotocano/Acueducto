package com.acueducto.veredal.invoices.infrastructure.payment;

import com.acueducto.veredal.invoices.application.InvoiceStatusService;
import com.acueducto.veredal.invoices.infrastructure.persistence.jpa.InvoiceEntity;
import com.acueducto.veredal.invoices.infrastructure.persistence.jpa.InvoiceRepository;
import com.acueducto.veredal.payments.application.port.InvoiceForPayment;
import com.acueducto.veredal.payments.application.port.InvoicePaymentPort;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class InvoicePaymentJpaAdapter implements InvoicePaymentPort {

    private final InvoiceRepository invoiceRepository;
    private final InvoiceStatusService invoiceStatusService;

    public InvoicePaymentJpaAdapter(InvoiceRepository invoiceRepository, InvoiceStatusService invoiceStatusService) {
        this.invoiceRepository = invoiceRepository;
        this.invoiceStatusService = invoiceStatusService;
    }

    @Override
    public InvoiceForPayment findForPayment(String tenantId, UUID invoiceId) {
        InvoiceEntity invoice = invoiceRepository.findByIdAndTenantId(invoiceId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found"));
        return toSnapshot(invoice);
    }

    @Override
    public void markPaidIfFullyCovered(String tenantId, UUID invoiceId, BigDecimal totalPaidAmount) {
        InvoiceEntity invoice = invoiceRepository.findByIdAndTenantId(invoiceId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found"));
        if (totalPaidAmount.compareTo(invoice.getTotalAmount()) >= 0
                && !"PAID".equalsIgnoreCase(invoice.getStatus())) {
            invoiceStatusService.changeStatus(invoice.getId(), "PAID", "Paid in full");
        }
    }

    private static InvoiceForPayment toSnapshot(InvoiceEntity invoice) {
        return new InvoiceForPayment(
                invoice.getId(),
                invoice.getWaterRightId(),
                invoice.getOwnerId(),
                invoice.getTotalAmount(),
                invoice.getStatus()
        );
    }
}
