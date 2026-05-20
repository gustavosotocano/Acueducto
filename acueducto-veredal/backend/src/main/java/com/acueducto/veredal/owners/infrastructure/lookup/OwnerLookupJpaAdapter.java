package com.acueducto.veredal.owners.infrastructure.lookup;

import com.acueducto.veredal.owners.infrastructure.persistence.jpa.OwnerEntity;
import com.acueducto.veredal.owners.infrastructure.persistence.jpa.OwnerRepository;
import com.acueducto.veredal.waterrights.application.port.OwnerLookupPort;
import com.acueducto.veredal.waterrights.application.port.OwnerSnapshot;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class OwnerLookupJpaAdapter implements OwnerLookupPort {

    private final OwnerRepository ownerRepository;

    public OwnerLookupJpaAdapter(OwnerRepository ownerRepository) {
        this.ownerRepository = ownerRepository;
    }

    @Override
    public OwnerSnapshot requireForWaterRight(String tenantId, UUID ownerId) {
        OwnerEntity owner = ownerRepository.findByIdAndTenantId(ownerId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Owner does not exist in this tenant"));
        return new OwnerSnapshot(owner.getId(), owner.getStatus());
    }
}
