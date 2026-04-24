CREATE TABLE payment (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    invoice_id UUID NOT NULL,
    water_right_id UUID NOT NULL,
    owner_id UUID NOT NULL,
    payment_date DATE NOT NULL,
    amount NUMERIC(14,2) NOT NULL,
    method VARCHAR(30) NOT NULL,
    reference VARCHAR(100),
    notes TEXT,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_payment_invoice FOREIGN KEY (invoice_id) REFERENCES invoice(id),
    CONSTRAINT fk_payment_water_right FOREIGN KEY (water_right_id) REFERENCES water_right(id),
    CONSTRAINT fk_payment_owner FOREIGN KEY (owner_id) REFERENCES owner(id),
    CONSTRAINT chk_payment_amount CHECK (amount > 0)
);

CREATE INDEX idx_payment_tenant ON payment (tenant_id);
CREATE INDEX idx_payment_tenant_invoice ON payment (tenant_id, invoice_id);
CREATE INDEX idx_payment_tenant_owner ON payment (tenant_id, owner_id);
CREATE INDEX idx_payment_tenant_water_right ON payment (tenant_id, water_right_id);
CREATE INDEX idx_payment_tenant_date ON payment (tenant_id, payment_date DESC);

