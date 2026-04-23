CREATE TABLE invoice (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    water_right_id UUID NOT NULL,
    owner_id UUID NOT NULL,
    invoice_number VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL,
    period_start DATE NOT NULL,
    period_end DATE NOT NULL,
    issue_date DATE NOT NULL,
    due_date DATE NOT NULL,
    total_amount NUMERIC(14,2) NOT NULL,
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_invoice_water_right FOREIGN KEY (water_right_id) REFERENCES water_right(id),
    CONSTRAINT fk_invoice_owner FOREIGN KEY (owner_id) REFERENCES owner(id),
    CONSTRAINT uk_invoice_tenant_number UNIQUE (tenant_id, invoice_number),
    CONSTRAINT chk_invoice_status CHECK (status IN ('DRAFT', 'ISSUED', 'PAID', 'OVERDUE', 'CANCELLED')),
    CONSTRAINT chk_invoice_period CHECK (period_end >= period_start),
    CONSTRAINT chk_invoice_amount CHECK (total_amount >= 0)
);

CREATE INDEX idx_invoice_tenant ON invoice (tenant_id);
CREATE INDEX idx_invoice_tenant_water_right ON invoice (tenant_id, water_right_id);
CREATE INDEX idx_invoice_tenant_owner ON invoice (tenant_id, owner_id);
CREATE INDEX idx_invoice_tenant_status_issue ON invoice (tenant_id, status, issue_date DESC);

