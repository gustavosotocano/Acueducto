package com.acueducto.veredal.waterrights.interfaces.rest.controller;

import com.acueducto.veredal.waterrights.application.WaterRightService;
import com.acueducto.veredal.waterrights.application.transfer.WaterRightTransferService;
import com.acueducto.veredal.waterrights.infrastructure.persistence.jpa.WaterRightEntity;
import com.acueducto.veredal.waterrights.infrastructure.persistence.jpa.history.WaterRightOwnerHistoryEntity;
import com.acueducto.veredal.waterrights.interfaces.rest.controller.transfer.TransferWaterRightRequest;
import com.acueducto.veredal.waterrights.interfaces.rest.controller.transfer.WaterRightOwnerHistoryResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/water-rights")
public class WaterRightController {

    private final WaterRightService waterRightService;
    private final WaterRightTransferService waterRightTransferService;

    public WaterRightController(
            WaterRightService waterRightService,
            WaterRightTransferService waterRightTransferService
    ) {
        this.waterRightService = waterRightService;
        this.waterRightTransferService = waterRightTransferService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public WaterRightResponse create(@Valid @RequestBody CreateWaterRightRequest request) {
        return toResponse(waterRightService.create(request));
    }

    @GetMapping
    public List<WaterRightResponse> findAll() {
        return waterRightService.findAll().stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public WaterRightResponse findById(@PathVariable UUID id) {
        return toResponse(waterRightService.findById(id));
    }

    @PutMapping("/{id}")
    public WaterRightResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateWaterRightRequest request) {
        return toResponse(waterRightService.update(id, request));
    }

    @PostMapping("/{id}/deactivate")
    public WaterRightResponse deactivate(@PathVariable UUID id, @Valid @RequestBody DeactivateWaterRightRequest request) {
        return toResponse(waterRightService.deactivate(id, request));
    }

    @PostMapping("/{id}/transfers")
    public WaterRightResponse transfer(@PathVariable UUID id, @Valid @RequestBody TransferWaterRightRequest request) {
        return toResponse(waterRightTransferService.transfer(id, request));
    }

    @GetMapping("/{id}/owner-history")
    public List<WaterRightOwnerHistoryResponse> findOwnerHistory(@PathVariable UUID id) {
        return waterRightTransferService.findHistory(id).stream().map(this::toHistoryResponse).toList();
    }

    private WaterRightResponse toResponse(WaterRightEntity entity) {
        return new WaterRightResponse(
                entity.getId(),
                entity.getTenantId(),
                entity.getRightCode(),
                entity.getOwnerId(),
                entity.getPropertyId(),
                entity.getWaterMeterId(),
                entity.getServiceStatus(),
                entity.getConnectionType(),
                entity.getBaseTariff(),
                entity.getOwnershipStartDate(),
                entity.getOwnershipEndDate(),
                entity.getCreatedAt(),
                entity.getUpdatedAt()
        );
    }

    private WaterRightOwnerHistoryResponse toHistoryResponse(WaterRightOwnerHistoryEntity entity) {
        return new WaterRightOwnerHistoryResponse(
                entity.getId(),
                entity.getTenantId(),
                entity.getWaterRightId(),
                entity.getOwnerId(),
                entity.getStartDate(),
                entity.getEndDate(),
                entity.getTransferReason(),
                entity.getNotes(),
                entity.getCreatedAt()
        );
    }
}
