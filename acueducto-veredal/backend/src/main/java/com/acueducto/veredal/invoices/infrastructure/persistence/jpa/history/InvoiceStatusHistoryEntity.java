package com.acueducto.veredal.invoices.infrastructure.persistence.jpa.history;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "invoice_status_history")
public class InvoiceStatusHistoryEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(name = "invoice_id", nullable = false)
    private UUID invoiceId;

    @Column(name = "from_status", length = 20)
    private String fromStatus;

    @Column(name = "to_status", nullable = false, length = 20)
    private String toStatus;

    @Column(name = "change_note", length = 250)
    private String changeNote;

    @Column(name = "changed_at", nullable = false)
    private OffsetDateTime changedAt;
}

