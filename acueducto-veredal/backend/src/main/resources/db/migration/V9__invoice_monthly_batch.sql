-- RF-060: idempotent monthly batch — at most one non-cancelled invoice per tenant, water_right, and period.
CREATE UNIQUE INDEX uq_invoice_tenant_wr_period_non_cancelled
    ON invoice (tenant_id, water_right_id, period_start, period_end)
    WHERE status <> 'CANCELLED';

CREATE TABLE invoice_number_counter (
    tenant_id VARCHAR(64) NOT NULL PRIMARY KEY,
    next_value BIGINT NOT NULL,
    CONSTRAINT chk_invoice_number_counter_positive CHECK (next_value >= 1)
);
