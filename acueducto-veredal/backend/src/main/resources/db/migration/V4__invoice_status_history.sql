CREATE TABLE invoice_status_history (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    invoice_id UUID NOT NULL,
    from_status VARCHAR(20),
    to_status VARCHAR(20) NOT NULL,
    change_note VARCHAR(250),
    changed_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_invoice_status_history_invoice FOREIGN KEY (invoice_id) REFERENCES invoice(id),
    CONSTRAINT chk_invoice_status_history_to CHECK (to_status IN ('DRAFT', 'ISSUED', 'PAID', 'OVERDUE', 'CANCELLED'))
);

CREATE INDEX idx_invoice_status_history_tenant_invoice ON invoice_status_history (tenant_id, invoice_id, changed_at DESC);

