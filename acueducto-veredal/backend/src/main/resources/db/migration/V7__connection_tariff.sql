CREATE TABLE connection_tariff (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    connection_type VARCHAR(30) NOT NULL,
    amount NUMERIC(14,2) NOT NULL,
    valid_from DATE NOT NULL,
    valid_to DATE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT chk_connection_tariff_amount CHECK (amount >= 0),
    CONSTRAINT chk_connection_tariff_validity CHECK (valid_to IS NULL OR valid_to >= valid_from)
);

CREATE INDEX idx_connection_tariff_tenant ON connection_tariff (tenant_id);
CREATE INDEX idx_connection_tariff_tenant_type_from ON connection_tariff (tenant_id, connection_type, valid_from DESC);
