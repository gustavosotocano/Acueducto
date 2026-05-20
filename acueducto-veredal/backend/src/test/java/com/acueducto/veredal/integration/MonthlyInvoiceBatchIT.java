package com.acueducto.veredal.integration;

import com.acueducto.veredal.support.IntegrationTestBase;
import com.acueducto.veredal.tenants.interfaces.rest.controller.CreateTenantRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MonthlyInvoiceBatchIT extends IntegrationTestBase {
    @Test
    void monthlyBatchIsIdempotentPerWaterRightAndPeriod() throws Exception {
        String tenant = "tenant_rf060_batch";
        createTenant(tenant);

        UUID ownerId = createOwner(tenant);
        UUID propertyId = createProperty(tenant, ownerId);
        createWaterRight(tenant, ownerId, propertyId, "WR-ACTIVE-1");

        String batchPayload = """
                {"billingYear":2026,"billingMonth":4,"issueDate":"2026-04-28","dueDate":"2026-05-15"}
                """;

        mockMvc.perform(withTenantAuth(tenant, post("/api/v1/invoices/batch/monthly")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(batchPayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.createdCount", is(1)))
                .andExpect(jsonPath("$.skippedCount", is(0)));

        mockMvc.perform(withTenantAuth(tenant, post("/api/v1/invoices/batch/monthly")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(batchPayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.createdCount", is(0)))
                .andExpect(jsonPath("$.skippedCount", is(1)))
                .andExpect(jsonPath("$.skipped[0].reason", is("INVOICE_ALREADY_EXISTS_FOR_PERIOD")));
    }

    private UUID createOwner(String tenant) throws Exception {
        String payload = objectMapper.writeValueAsString(new OwnerPayload(
                "CC",
                "RF060-1",
                "Ana",
                "Lopez",
                "3000000000",
                "ana.lopez@example.com"
        ));
        String body = mockMvc.perform(withTenantAuth(tenant, post("/api/v1/owners")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return UUID.fromString(objectMapper.readTree(body).get("id").asText());
    }

    private UUID createProperty(String tenant, UUID ownerId) throws Exception {
        String payload = objectMapper.writeValueAsString(new PropertyPayload(
                "P-RF060-1",
                "Predio RF060",
                "Calle 1",
                ownerId,
                LocalDate.of(2026, 1, 1)
        ));
        String body = mockMvc.perform(withTenantAuth(tenant, post("/api/v1/properties")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return UUID.fromString(objectMapper.readTree(body).get("id").asText());
    }

    private UUID createWaterRight(String tenant, UUID ownerId, UUID propertyId, String rightCode) throws Exception {
        String payload = objectMapper.writeValueAsString(new WaterRightPayload(
                rightCode,
                ownerId,
                propertyId,
                null,
                "ACTIVE",
                "RESIDENTIAL",
                new BigDecimal("25000.00"),
                LocalDate.of(2026, 1, 1)
        ));
        String body = mockMvc.perform(withTenantAuth(tenant, post("/api/v1/water-rights")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return UUID.fromString(objectMapper.readTree(body).get("id").asText());
    }

    private record OwnerPayload(
            String documentType,
            String documentNumber,
            String firstName,
            String lastName,
            String phone,
            String email
    ) {
    }

    private record PropertyPayload(
            String propertyCode,
            String propertyName,
            String addressLine,
            UUID ownerId,
            LocalDate ownershipStartDate
    ) {
    }

    private record WaterRightPayload(
            String rightCode,
            UUID ownerId,
            UUID propertyId,
            UUID waterMeterId,
            String serviceStatus,
            String connectionType,
            BigDecimal baseTariff,
            LocalDate ownershipStartDate
    ) {
    }
}
