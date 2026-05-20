package com.acueducto.veredal.properties.infrastructure.lookup;

import com.acueducto.veredal.properties.infrastructure.persistence.jpa.PropertyEntity;
import com.acueducto.veredal.properties.infrastructure.persistence.jpa.PropertyRepository;
import com.acueducto.veredal.waterrights.application.port.PropertyLookupPort;
import com.acueducto.veredal.waterrights.application.port.PropertySnapshot;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class PropertyLookupJpaAdapter implements PropertyLookupPort {

    private final PropertyRepository propertyRepository;

    public PropertyLookupJpaAdapter(PropertyRepository propertyRepository) {
        this.propertyRepository = propertyRepository;
    }

    @Override
    public PropertySnapshot requireForWaterRight(String tenantId, UUID propertyId) {
        PropertyEntity property = propertyRepository.findByIdAndTenantId(propertyId, tenantId)
                .orElseThrow(() -> new IllegalArgumentException("Property does not exist in this tenant"));
        return new PropertySnapshot(property.getId(), property.getStatus(), property.getOwnerId());
    }
}
