package com.acueducto.veredal.tariffs.application;

import com.acueducto.veredal.shared.multitenancy.TenantContext;
import com.acueducto.veredal.tariffs.infrastructure.persistence.jpa.ConnectionTariffEntity;
import com.acueducto.veredal.tariffs.infrastructure.persistence.jpa.ConnectionTariffRepository;
import com.acueducto.veredal.tariffs.interfaces.rest.controller.CreateConnectionTariffRequest;
import com.acueducto.veredal.tariffs.interfaces.rest.controller.UpdateConnectionTariffRequest;
import com.acueducto.veredal.waterrights.domain.ConnectionType;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class ConnectionTariffService {

    private static final LocalDate MAX_DATE = LocalDate.of(9999, 12, 31);

    private final ConnectionTariffRepository repository;

    public ConnectionTariffService(ConnectionTariffRepository repository) {
        this.repository = repository;
    }

    public ConnectionTariffEntity create(CreateConnectionTariffRequest request) {
        String tenantId = TenantContext.getRequiredTenantId();
        validateValidity(request.validFrom(), request.validTo());
        assertNoOverlap(tenantId, request.connectionType(), request.validFrom(), request.validTo(), null);

        OffsetDateTime now = OffsetDateTime.now();
        ConnectionTariffEntity e = new ConnectionTariffEntity();
        e.setId(UUID.randomUUID());
        e.setTenantId(tenantId);
        e.setConnectionType(request.connectionType());
        e.setAmount(request.amount());
        e.setValidFrom(request.validFrom());
        e.setValidTo(request.validTo());
        e.setCreatedAt(now);
        e.setUpdatedAt(now);
        return repository.save(e);
    }

    @Transactional(readOnly = true)
    public List<ConnectionTariffEntity> findAll() {
        return repository.findAllByTenantIdOrderByConnectionTypeAscValidFromDesc(TenantContext.getRequiredTenantId());
    }

    @Transactional(readOnly = true)
    public ConnectionTariffEntity findById(UUID id) {
        return repository.findByIdAndTenantId(id, TenantContext.getRequiredTenantId())
                .orElseThrow(() -> new IllegalArgumentException("Connection tariff not found"));
    }

    public ConnectionTariffEntity update(UUID id, UpdateConnectionTariffRequest request) {
        String tenantId = TenantContext.getRequiredTenantId();
        validateValidity(request.validFrom(), request.validTo());

        ConnectionTariffEntity existing = repository.findByIdAndTenantId(id, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Connection tariff not found"));

        assertNoOverlap(tenantId, existing.getConnectionType(), request.validFrom(), request.validTo(), id);

        OffsetDateTime now = OffsetDateTime.now();
        existing.setAmount(request.amount());
        existing.setValidFrom(request.validFrom());
        existing.setValidTo(request.validTo());
        existing.setUpdatedAt(now);
        return repository.save(existing);
    }

    @Transactional(readOnly = true)
    public ConnectionTariffEntity findApplicable(ConnectionType connectionType, LocalDate onDate) {
        String tenantId = TenantContext.getRequiredTenantId();
        return repository.findApplicable(tenantId, connectionType, onDate, PageRequest.of(0, 1))
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No applicable tariff found for connection type"));
    }

    private void validateValidity(LocalDate from, LocalDate to) {
        if (to != null && to.isBefore(from)) {
            throw new IllegalArgumentException("validTo cannot be before validFrom");
        }
    }

    private void assertNoOverlap(
            String tenantId,
            ConnectionType connectionType,
            LocalDate from,
            LocalDate to,
            UUID excludeId
    ) {
        LocalDate effectiveTo = to != null ? to : MAX_DATE;
        List<ConnectionTariffEntity> overlaps = excludeId == null
                ? repository.findOverlaps(tenantId, connectionType, from, effectiveTo, MAX_DATE)
                : repository.findOverlapsExcludingId(tenantId, connectionType, from, effectiveTo, MAX_DATE, excludeId);
        if (!overlaps.isEmpty()) {
            throw new IllegalArgumentException("Tariff validity overlaps with an existing tariff for this connection type");
        }
    }
}

