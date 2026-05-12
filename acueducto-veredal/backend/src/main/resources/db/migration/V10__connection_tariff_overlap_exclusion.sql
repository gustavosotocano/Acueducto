-- RF-065: enforce non-overlapping validity per tenant + connection_type (matches application rule; null valid_to = open-ended to 9999-12-31).
CREATE EXTENSION IF NOT EXISTS btree_gist;

ALTER TABLE connection_tariff
    ADD CONSTRAINT ex_connection_tariff_no_overlap
        EXCLUDE USING gist (
            tenant_id WITH =,
            connection_type WITH =,
            daterange(valid_from, COALESCE(valid_to, DATE '9999-12-31'), '[]') WITH &&
        );
