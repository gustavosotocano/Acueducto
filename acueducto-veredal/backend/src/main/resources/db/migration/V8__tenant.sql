CREATE TABLE tenant (
    id VARCHAR(64) PRIMARY KEY,
    tenant_name VARCHAR(200) NOT NULL,
    contact_name VARCHAR(200),
    contact_phone VARCHAR(30),
    contact_email VARCHAR(150),
    status VARCHAR(20) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_tenant_name UNIQUE (tenant_name),
    CONSTRAINT chk_tenant_status CHECK (status IN ('ACTIVE', 'INACTIVE'))
);

CREATE INDEX idx_tenant_status ON tenant (status);
