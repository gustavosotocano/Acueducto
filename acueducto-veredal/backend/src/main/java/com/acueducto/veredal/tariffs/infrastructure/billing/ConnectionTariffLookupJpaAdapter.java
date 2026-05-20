package com.acueducto.veredal.tariffs.infrastructure.billing;

import com.acueducto.veredal.invoices.application.port.ApplicableTariffSnapshot;
import com.acueducto.veredal.invoices.application.port.ConnectionTariffLookupPort;
import com.acueducto.veredal.tariffs.infrastructure.persistence.jpa.ConnectionTariffEntity;
import com.acueducto.veredal.tariffs.infrastructure.persistence.jpa.ConnectionTariffRepository;
import com.acueducto.veredal.waterrights.domain.ConnectionType;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Optional;

@Component
public class ConnectionTariffLookupJpaAdapter implements ConnectionTariffLookupPort {

    private final ConnectionTariffRepository connectionTariffRepository;

    public ConnectionTariffLookupJpaAdapter(ConnectionTariffRepository connectionTariffRepository) {
        this.connectionTariffRepository = connectionTariffRepository;
    }

    @Override
    public Optional<ApplicableTariffSnapshot> findApplicable(String tenantId, String connectionType, LocalDate onDate) {
        ConnectionType type = ConnectionType.valueOf(connectionType);
        return connectionTariffRepository
                .findApplicable(tenantId, type, onDate, PageRequest.of(0, 1))
                .stream()
                .findFirst()
                .map(ConnectionTariffLookupJpaAdapter::toSnapshot);
    }

    private static ApplicableTariffSnapshot toSnapshot(ConnectionTariffEntity entity) {
        return new ApplicableTariffSnapshot(
                entity.getAmount(),
                entity.getValidFrom(),
                entity.getValidTo()
        );
    }
}
