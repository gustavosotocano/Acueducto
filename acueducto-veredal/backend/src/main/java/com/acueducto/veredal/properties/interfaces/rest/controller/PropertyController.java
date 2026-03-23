package com.acueducto.veredal.properties.interfaces.rest.controller;

import com.acueducto.veredal.properties.application.PropertyService;
import com.acueducto.veredal.properties.application.ownership.PropertyOwnershipService;
import com.acueducto.veredal.properties.infrastructure.persistence.jpa.PropertyEntity;
import com.acueducto.veredal.properties.infrastructure.persistence.jpa.history.PropertyOwnerHistoryEntity;
import com.acueducto.veredal.properties.interfaces.rest.controller.ownership.ChangePropertyOwnerRequest;
import com.acueducto.veredal.properties.interfaces.rest.controller.ownership.PropertyOwnerHistoryResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/properties")
public class PropertyController {

    private final PropertyService propertyService;
    private final PropertyOwnershipService propertyOwnershipService;

    public PropertyController(PropertyService propertyService, PropertyOwnershipService propertyOwnershipService) {
        this.propertyService = propertyService;
        this.propertyOwnershipService = propertyOwnershipService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PropertyResponse create(@Valid @RequestBody CreatePropertyRequest request) {
        return toResponse(propertyService.create(request));
    }

    @GetMapping
    public List<PropertyResponse> findAll() {
        return propertyService.findAll().stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public PropertyResponse findById(@PathVariable UUID id) {
        return toResponse(propertyService.findById(id));
    }

    @PutMapping("/{id}")
    public PropertyResponse update(@PathVariable UUID id, @Valid @RequestBody UpdatePropertyRequest request) {
        return toResponse(propertyService.update(id, request));
    }

    @PostMapping("/{id}/deactivate")
    public PropertyResponse deactivate(@PathVariable UUID id) {
        return toResponse(propertyService.deactivate(id));
    }

    @PostMapping("/{id}/owner-changes")
    public PropertyResponse changeOwner(@PathVariable UUID id, @Valid @RequestBody ChangePropertyOwnerRequest request) {
        return toResponse(propertyOwnershipService.changeOwner(id, request));
    }

    @GetMapping("/{id}/owner-history")
    public List<PropertyOwnerHistoryResponse> findOwnerHistory(@PathVariable UUID id) {
        return propertyOwnershipService.findHistory(id).stream().map(this::toHistoryResponse).toList();
    }

    private PropertyResponse toResponse(PropertyEntity entity) {
        return new PropertyResponse(
                entity.getId(),
                entity.getTenantId(),
                entity.getPropertyCode(),
                entity.getPropertyName(),
                entity.getAddressLine(),
                entity.getOwnerId(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private PropertyOwnerHistoryResponse toHistoryResponse(PropertyOwnerHistoryEntity entity) {
        return new PropertyOwnerHistoryResponse(
                entity.getId(),
                entity.getTenantId(),
                entity.getPropertyId(),
                entity.getOwnerId(),
                entity.getStartDate(),
                entity.getEndDate(),
                entity.getChangeReason(),
                entity.getNotes(),
                entity.getCreatedAt()
        );
    }
}
