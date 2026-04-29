package com.acueducto.veredal.tenants.interfaces.rest.controller;

import com.acueducto.veredal.tenants.application.TenantService;
import com.acueducto.veredal.tenants.infrastructure.persistence.jpa.TenantEntity;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/tenants")
public class TenantController {

    private final TenantService service;

    public TenantController(TenantService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TenantResponse create(@Valid @RequestBody CreateTenantRequest request) {
        return toResponse(service.create(request));
    }

    @GetMapping
    public List<TenantResponse> findAll() {
        return service.findAll().stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public TenantResponse findById(@PathVariable String id) {
        return toResponse(service.findById(id));
    }

    @PutMapping("/{id}")
    public TenantResponse update(@PathVariable String id, @Valid @RequestBody UpdateTenantRequest request) {
        return toResponse(service.update(id, request));
    }

    @PostMapping("/{id}/deactivate")
    public TenantResponse deactivate(@PathVariable String id) {
        return toResponse(service.deactivate(id));
    }

    private TenantResponse toResponse(TenantEntity t) {
        return new TenantResponse(
                t.getId(),
                t.getTenantName(),
                t.getContactName(),
                t.getContactPhone(),
                t.getContactEmail(),
                t.getStatus(),
                t.getCreatedAt(),
                t.getUpdatedAt()
        );
    }
}

