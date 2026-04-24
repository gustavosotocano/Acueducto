CREATE TABLE interest_config (
    tenant_id VARCHAR(64) PRIMARY KEY,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE interest_tier (
    id UUID PRIMARY KEY,
    tenant_id VARCHAR(64) NOT NULL,
    day_from INT NOT NULL,
    day_to INT,
    rate_percent NUMERIC(6,3) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT fk_interest_tier_config FOREIGN KEY (tenant_id) REFERENCES interest_config(tenant_id),
    CONSTRAINT chk_interest_tier_days CHECK (day_from >= 1 AND (day_to IS NULL OR day_to >= day_from)),
    CONSTRAINT chk_interest_tier_rate CHECK (rate_percent >= 0)
);

CREATE INDEX idx_interest_tier_tenant_days ON interest_tier (tenant_id, day_from, day_to);

