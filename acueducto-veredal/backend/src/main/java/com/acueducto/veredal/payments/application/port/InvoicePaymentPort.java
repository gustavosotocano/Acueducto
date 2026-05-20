package com.acueducto.veredal.payments.application.port;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Puerto de salida: consultas y efectos sobre facturas necesarios para pagos.
 * Implementación JPA vive en el módulo {@code invoices}.
 */
public interface InvoicePaymentPort {

    InvoiceForPayment findForPayment(String tenantId, UUID invoiceId);

    /**
     * Si el total pagado cubre la factura y aún no está {@code PAID}, cambia estado e historial.
     */
    void markPaidIfFullyCovered(String tenantId, UUID invoiceId, BigDecimal totalPaidAmount);
}
