package com.acueducto.veredal.waterrights.application.port;

import java.util.UUID;

public interface PropertyLookupPort {

    PropertySnapshot requireForWaterRight(String tenantId, UUID propertyId);
}
