package com.acueducto.veredal.payments.interfaces.rest.controller;

import com.acueducto.veredal.payments.application.PaymentService;
import com.acueducto.veredal.payments.infrastructure.persistence.jpa.PaymentEntity;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('ADMIN', 'TREASURY')")
    public PaymentResponse create(@Valid @RequestBody CreatePaymentRequest request) {
        return toResponse(paymentService.create(request));
    }

    @GetMapping
    public List<PaymentResponse> findAll(
            @RequestParam(required = false) UUID invoiceId,
            @RequestParam(required = false) UUID waterRightId,
            @RequestParam(required = false) UUID ownerId,
            @RequestParam(required = false) LocalDate from,
            @RequestParam(required = false) LocalDate to
    ) {
        return paymentService.findAll(invoiceId, waterRightId, ownerId, from, to).stream().map(this::toResponse).toList();
    }

    private PaymentResponse toResponse(PaymentEntity entity) {
        return new PaymentResponse(
                entity.getId(),
                entity.getTenantId(),
                entity.getInvoiceId(),
                entity.getWaterRightId(),
                entity.getOwnerId(),
                entity.getPaymentDate(),
                entity.getAmount(),
                entity.getMethod(),
                entity.getReference(),
                entity.getNotes(),
                entity.getCreatedAt()
        );
    }
}

