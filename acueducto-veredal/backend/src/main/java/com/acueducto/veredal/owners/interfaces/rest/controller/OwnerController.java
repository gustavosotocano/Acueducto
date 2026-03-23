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

    private OwnerResponse toResponse(OwnerEntity entity) {
        return new OwnerResponse(
                entity.getId(),
                entity.getTenantId(),
                entity.getDocumentType(),
                entity.getDocumentNumber(),
                entity.getFullName(),
                entity.getPhone(),
                entity.getEmail(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }
}
