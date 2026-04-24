package com.acueducto.veredal.invoices.interfaces.rest.controller;

import com.acueducto.veredal.invoices.application.InvoiceService;
import com.acueducto.veredal.invoices.application.InvoiceStatusService;
import com.acueducto.veredal.invoices.infrastructure.persistence.jpa.InvoiceEntity;
import com.acueducto.veredal.invoices.infrastructure.persistence.jpa.history.InvoiceStatusHistoryEntity;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;
    private final InvoiceStatusService invoiceStatusService;

    public InvoiceController(InvoiceService invoiceService, InvoiceStatusService invoiceStatusService) {
        this.invoiceService = invoiceService;
        this.invoiceStatusService = invoiceStatusService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public InvoiceResponse create(@Valid @RequestBody CreateInvoiceRequest request) {
        return toResponse(invoiceService.create(request));
    }

    @GetMapping
    public List<InvoiceResponse> findAll(
            @RequestParam(required = false) UUID waterRightId,
            @RequestParam(required = false) UUID ownerId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) LocalDate issueFrom,
            @RequestParam(required = false) LocalDate issueTo
    ) {
        return invoiceService.findAll(waterRightId, ownerId, status, issueFrom, issueTo).stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public InvoiceResponse findById(@PathVariable UUID id) {
        return toResponse(invoiceService.findById(id));
    }

    @PostMapping("/{id}/status")
    public InvoiceResponse changeStatus(@PathVariable UUID id, @Valid @RequestBody ChangeInvoiceStatusRequest request) {
        return toResponse(invoiceStatusService.changeStatus(id, request.status(), request.note()));
    }

    @GetMapping("/{id}/status-history")
    public List<InvoiceStatusHistoryResponse> statusHistory(@PathVariable UUID id) {
        return invoiceStatusService.history(id).stream().map(this::toHistoryResponse).toList();
    }

    private InvoiceResponse toResponse(InvoiceEntity entity) {
        return new InvoiceResponse(
                entity.getId(),
                entity.getTenantId(),
                entity.getWaterRightId(),
                entity.getOwnerId(),
                entity.getInvoiceNumber(),
                entity.getStatus(),
                entity.getPeriodStart(),
                entity.getPeriodEnd(),
                entity.getIssueDate(),
                entity.getDueDate(),
                entity.getTotalAmount(),
                entity.getNotes(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private InvoiceStatusHistoryResponse toHistoryResponse(InvoiceStatusHistoryEntity entity) {
        return new InvoiceStatusHistoryResponse(
                entity.getId(),
                entity.getTenantId(),
                entity.getInvoiceId(),
                entity.getFromStatus(),
                entity.getToStatus(),
                entity.getChangeNote(),
                entity.getChangedAt()
        );
    }
}

