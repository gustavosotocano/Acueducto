package com.acueducto.veredal.invoices.application.port;

import java.util.List;
import java.util.UUID;

public interface WaterRightBillingPort {

    WaterRightBillingSnapshot findForBilling(String tenantId, UUID waterRightId);

    List<WaterRightBillingSnapshot> findActiveForMonthlyBatch(String tenantId);
}
