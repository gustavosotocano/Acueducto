package com.acueducto.veredal.payments.application;

import com.acueducto.veredal.invoices.application.InvoiceStatusService;
import com.acueducto.veredal.invoices.infrastructure.persistence.jpa.InvoiceEntity;
import com.acueducto.veredal.invoices.infrastructure.persistence.jpa.InvoiceRepository;
import com.acueducto.veredal.payments.infrastructure.persistence.jpa.PaymentEntity;
import com.acueducto.veredal.payments.infrastructure.persistence.jpa.PaymentRepository;
import com.acueducto.veredal.payments.interfaces.rest.controller.CreatePaymentRequest;
import com.acueducto.veredal.shared.multitenancy.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final InvoiceRepository invoiceRepository;
    private final InvoiceStatusService invoiceStatusService;

    public PaymentService(
            PaymentRepository paymentRepository,
            InvoiceRepository invoiceRepository,
            InvoiceStatusService invoiceStatusService
    ) {
        this.paymentRepository = paymentRepository;
        this.invoiceRepository = invoiceRepository;
        this.invoiceStatusService = invoiceStatusService;
    }

    public PaymentEntity create(CreatePaymentRequest request) {
        String tenantId = TenantContext.getRequiredTenantId();

        InvoiceEntity invoice = invoiceRepository.findByIdAndTenantId(request.invoiceId(), tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Invoice not found"));

        if ("CANCELLED".equalsIgnoreCase(invoice.getStatus())) {
            throw new IllegalStateException("Cannot register payments for a cancelled invoice");
        }

        BigDecimal paidBefore = paymentRepository.sumAmountByTenantIdAndInvoiceId(tenantId, invoice.getId());
        BigDecimal outstanding = invoice.getTotalAmount().subtract(paidBefore);
        if (outstanding.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Invoice has no outstanding balance");
        }
        if (request.amount().compareTo(outstanding) > 0) {
            throw new IllegalArgumentException("Payment amount exceeds invoice outstanding balance");
        }

        OffsetDateTime now = OffsetDateTime.now();
        PaymentEntity payment = new PaymentEntity();
        payment.setId(UUID.randomUUID());
        payment.setTenantId(tenantId);
        payment.setInvoiceId(invoice.getId());
        payment.setWaterRightId(invoice.getWaterRightId());
        payment.setOwnerId(invoice.getOwnerId());
        payment.setPaymentDate(request.paymentDate());
        payment.setAmount(request.amount());
        payment.setMethod(request.method());
        payment.setReference(request.reference());
        payment.setNotes(request.notes());
        payment.setCreatedAt(now);

        PaymentEntity saved = paymentRepository.save(payment);
        applyPaymentsToInvoice(tenantId, invoice);
        return saved;
    }

    @Transactional(readOnly = true)
    public List<PaymentEntity> findAll(UUID invoiceId, UUID waterRightId, UUID ownerId, LocalDate from, LocalDate to) {
        String tenantId = TenantContext.getRequiredTenantId();

        if (invoiceId != null) {
            return paymentRepository.findAllByTenantIdAndInvoiceIdOrderByPaymentDateDesc(tenantId, invoiceId);
        }
        if (waterRightId != null) {
            return paymentRepository.findAllByTenantIdAndWaterRightIdOrderByPaymentDateDesc(tenantId, waterRightId);
        }
        if (ownerId != null) {
            return paymentRepository.findAllByTenantIdAndOwnerIdOrderByPaymentDateDesc(tenantId, ownerId);
        }
        if (from != null && to != null) {
            return paymentRepository.findAllByTenantIdAndPaymentDateBetweenOrderByPaymentDateDesc(tenantId, from, to);
        }
        return paymentRepository.findAllByTenantIdOrderByPaymentDateDesc(tenantId);
    }

    private void applyPaymentsToInvoice(String tenantId, InvoiceEntity invoice) {
        BigDecimal paid = paymentRepository.sumAmountByTenantIdAndInvoiceId(tenantId, invoice.getId());
        if (paid.compareTo(invoice.getTotalAmount()) >= 0 && !"PAID".equalsIgnoreCase(invoice.getStatus())) {
            invoiceStatusService.changeStatus(invoice.getId(), "PAID", "Paid in full");
        }
    }
}

