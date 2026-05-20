package com.acueducto.veredal.waterrights.application.port;

import java.util.UUID;

public interface OwnerLookupPort {

    OwnerSnapshot requireForWaterRight(String tenantId, UUID ownerId);
}
