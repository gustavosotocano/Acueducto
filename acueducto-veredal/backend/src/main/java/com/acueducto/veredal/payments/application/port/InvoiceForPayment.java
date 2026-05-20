package com.acueducto.veredal.payments.application.port;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Snapshot mínimo de factura para registrar pagos (sin entidades JPA del módulo invoices).
 */
public record InvoiceForPayment(
        UUID id,
        UUID waterRightId,
        UUID ownerId,
        BigDecimal totalAmount,
        String status
) {
}
