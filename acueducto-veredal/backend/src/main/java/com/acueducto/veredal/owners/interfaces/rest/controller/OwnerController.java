package com.acueducto.veredal.owners.interfaces.rest.controller;

import com.acueducto.veredal.owners.application.OwnerService;
import com.acueducto.veredal.owners.infrastructure.persistence.jpa.OwnerEntity;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/owners")
public class OwnerController {

    private final OwnerService ownerService;

    public OwnerController(OwnerService ownerService) {
        this.ownerService = ownerService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OwnerResponse create(@Valid @RequestBody CreateOwnerRequest request) {
        return toResponse(ownerService.create(request));
    }

    @GetMapping
    public List<OwnerResponse> findAll() {
        return ownerService.findAll().stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public OwnerResponse findById(@PathVariable UUID id) {
        return toResponse(ownerService.findById(id));
    }

    @PutMapping("/{id}")
    public OwnerResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateOwnerRequest request) {
        return toResponse(ownerService.update(id, request));
    }

    @PostMapping("/{id}/deactivate")
    public OwnerResponse deactivate(@PathVariable UUID id) {
        return toResponse(ownerService.deactivate(id));
    }

    private OwnerResponse toResponse(OwnerEntity entity) {
        String fullName = coalesceFullName(entity);
        return new OwnerResponse(
                entity.getId(),
                entity.getTenantId(),
                entity.getDocumentType(),
                entity.getDocumentNumber(),
                fullName,
                entity.getFirstName(),
                entity.getLastName(),
                entity.getSecondLastName(),
                entity.getPhone(),
                entity.getEmail(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private String coalesceFullName(OwnerEntity entity) {
        String first = safe(entity.getFirstName());
        String last = safe(entity.getLastName());
        String secondLast = safe(entity.getSecondLastName());

        if (!first.isBlank() && !last.isBlank()) {
            StringBuilder sb = new StringBuilder();
            sb.append(first).append(" ").append(last);
            if (!secondLast.isBlank()) sb.append(" ").append(secondLast);
            return sb.toString();
        }

        return safe(entity.getFullName());
    }

    private String safe(String v) {
        return v == null ? "" : v.trim();
    }
}
