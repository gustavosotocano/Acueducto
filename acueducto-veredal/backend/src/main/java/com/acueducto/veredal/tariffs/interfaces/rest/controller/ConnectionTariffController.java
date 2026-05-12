package com.acueducto.veredal.tariffs.interfaces.rest.controller;

import com.acueducto.veredal.tariffs.application.ConnectionTariffService;
import com.acueducto.veredal.tariffs.infrastructure.persistence.jpa.ConnectionTariffEntity;
import com.acueducto.veredal.waterrights.domain.ConnectionType;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/connection-tariffs")
public class ConnectionTariffController {

    private final ConnectionTariffService service;

    public ConnectionTariffController(ConnectionTariffService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ConnectionTariffResponse create(@Valid @RequestBody CreateConnectionTariffRequest request) {
        return toResponse(service.create(request));
    }

    @GetMapping
    public List<ConnectionTariffResponse> findAll() {
        return service.findAll().stream().map(this::toResponse).toList();
    }

    /**
     * Resolves the tariff row used for billing on {@code onDate} (same rule as invoice total: latest {@code validFrom}
     * among rows where validFrom &lt;= onDate &lt;= coalesce(validTo, infinity)).
     */
    @GetMapping("/applicable")
    public ResponseEntity<ConnectionTariffResponse> applicable(
            @RequestParam ConnectionType connectionType,
            @RequestParam LocalDate onDate
    ) {
        return service.findApplicableOptional(connectionType, onDate)
                .map(this::toResponse)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}")
    public ConnectionTariffResponse findById(@PathVariable UUID id) {
        return toResponse(service.findById(id));
    }

    @PutMapping("/{id}")
    public ConnectionTariffResponse update(@PathVariable UUID id, @Valid @RequestBody UpdateConnectionTariffRequest request) {
        return toResponse(service.update(id, request));
    }

    private ConnectionTariffResponse toResponse(ConnectionTariffEntity e) {
        return new ConnectionTariffResponse(
                e.getId(),
                e.getTenantId(),
                e.getConnectionType(),
                e.getAmount(),
                e.getValidFrom(),
                e.getValidTo(),
                e.getCreatedAt(),
                e.getUpdatedAt()
        );
    }
}

