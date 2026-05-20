package com.acueducto.veredal.portfolio.application.port;

import java.util.List;

/**
 * Lectura de cartera (facturas + pagos). Implementación JPA en el módulo {@code invoices}.
 */
public interface PortfolioReadPort {

    List<WaterRightBalanceSnapshot> findOutstandingByWaterRight(String tenantId);

    List<OwnerBalanceSnapshot> findOutstandingByOwner(String tenantId);

    List<OverdueBucketSnapshot> findOverdueSummary(String tenantId);

    List<OverdueInvoiceSnapshot> findOverdueInvoices(String tenantId);
}
