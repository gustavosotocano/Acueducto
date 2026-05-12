package com.acueducto.veredal.invoices.application;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.Locale;

@Service
public class InvoiceNumberAllocator {

    private final JdbcTemplate jdbcTemplate;

    public InvoiceNumberAllocator(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Atomically reserves the next invoice number for the tenant (RF-055-style FAC- prefix + 6 digits).
     */
    public String allocateNext(String tenantId) {
        Long seq = jdbcTemplate.queryForObject(
                """
                        INSERT INTO invoice_number_counter (tenant_id, next_value)
                        VALUES (?, 1)
                        ON CONFLICT (tenant_id) DO UPDATE
                            SET next_value = invoice_number_counter.next_value + 1
                        RETURNING next_value
                        """,
                Long.class,
                tenantId
        );
        if (seq == null) {
            throw new IllegalStateException("Could not allocate invoice number");
        }
        return "FAC-" + String.format(Locale.US, "%06d", seq);
    }
}
