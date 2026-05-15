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
class PaymentInvoiceRequiredIT extends IntegrationTestBase {
    @Test
    void paymentRequiresInvoiceId() throws Exception {
        String tenant = "tenant_rf059_missing_invoice";
        createTenant(tenant);

        String payload = """
                {
                  "paymentDate":"2026-04-28",
                  "amount":25000.00,
                  "method":"CASH"
                }
                """;

        mockMvc.perform(withTenantAuth(tenant, post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("invoiceId must not be null")));
    }

    @Test
    void paymentRejectsUnknownInvoice() throws Exception {
        String tenant = "tenant_rf059_unknown_invoice";
        createTenant(tenant);

        String payload = """
                {
                  "invoiceId":"%s",
                  "paymentDate":"2026-04-28",
                  "amount":25000.00,
                  "method":"CASH"
                }
                """.formatted(UUID.randomUUID());

        mockMvc.perform(withTenantAuth(tenant, post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Invoice not found")));
    }

    @Test
    void paymentRegistersAfterInvoiceCreated() throws Exception {
        String tenant = "tenant_rf059_happy_path";
        createTenant(tenant);

        UUID ownerId = createOwner(tenant);
        UUID propertyId = createProperty(tenant, ownerId);
        UUID waterRightId = createWaterRight(tenant, ownerId, propertyId, "WR-RF059-1");
        UUID invoiceId = createInvoice(tenant, waterRightId, "FAC-RF059-1");

        String payload = """
                {
                  "invoiceId":"%s",
                  "paymentDate":"2026-04-28",
                  "amount":25000.00,
                  "method":"TRANSFER",
                  "reference":"TRX-059"
                }
                """.formatted(invoiceId);

        mockMvc.perform(withTenantAuth(tenant, post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.invoiceId", is(invoiceId.toString())))
                .andExpect(jsonPath("$.waterRightId", is(waterRightId.toString())))
                .andExpect(jsonPath("$.ownerId", is(ownerId.toString())))
                .andExpect(jsonPath("$.amount", is(25000.00)))
                .andExpect(jsonPath("$.method", is("TRANSFER")));
    }

    @Test
    void partialPaymentLeavesOutstandingBalance() throws Exception {
        String tenant = "tenant_rf059_partial";
        createTenant(tenant);

        UUID ownerId = createOwner(tenant);
        UUID propertyId = createProperty(tenant, ownerId);
        UUID waterRightId = createWaterRight(tenant, ownerId, propertyId, "WR-RF059-PARTIAL");
        UUID invoiceId = createInvoice(tenant, waterRightId, "FAC-RF059-PARTIAL");

        registerPayment(tenant, invoiceId, "2026-04-28", "10000.00", "CASH");
        registerPayment(tenant, invoiceId, "2026-04-29", "15000.00", "TRANSFER");
    }

    @Test
    void paymentRejectsAmountAboveOutstandingBalance() throws Exception {
        String tenant = "tenant_rf059_overpay";
        createTenant(tenant);

        UUID ownerId = createOwner(tenant);
        UUID propertyId = createProperty(tenant, ownerId);
        UUID waterRightId = createWaterRight(tenant, ownerId, propertyId, "WR-RF059-OVER");
        UUID invoiceId = createInvoice(tenant, waterRightId, "FAC-RF059-OVER");

        registerPayment(tenant, invoiceId, "2026-04-28", "10000.00", "CASH");

        String payload = """
                {
                  "invoiceId":"%s",
                  "paymentDate":"2026-04-29",
                  "amount":20000.00,
                  "method":"TRANSFER"
                }
                """.formatted(invoiceId);

        mockMvc.perform(withTenantAuth(tenant, post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Payment amount exceeds invoice outstanding balance")));
    }

    private void registerPayment(String tenant, UUID invoiceId, String paymentDate, String amount, String method)
            throws Exception {
        String payload = """
                {
                  "invoiceId":"%s",
                  "paymentDate":"%s",
                  "amount":%s,
                  "method":"%s"
                }
                """.formatted(invoiceId, paymentDate, amount, method);

        mockMvc.perform(withTenantAuth(tenant, post("/api/v1/payments")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload)))
                .andExpect(status().isCreated());
    }

    private void createTenant(String id) throws Exception {
        String body = objectMapper.writeValueAsString(new CreateTenantRequest(
                id,
                "Tenant " + id,
                null,
                null,
                null
        ));
        mockMvc.perform(withPlatformAuth(post("/api/v1/tenants"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
    }

    private UUID createOwner(String tenant) throws Exception {
        String payload = objectMapper.writeValueAsString(new OwnerPayload(
                "CC",
                "RF059-1",
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
                "P-RF059-1",
                "Predio RF059",
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

    private UUID createInvoice(String tenant, UUID waterRightId, String invoiceNumber) throws Exception {
        String payload = objectMapper.writeValueAsString(new InvoicePayload(
                waterRightId,
                invoiceNumber,
                LocalDate.of(2026, 4, 1),
                LocalDate.of(2026, 4, 30),
                LocalDate.of(2026, 4, 28),
                LocalDate.of(2026, 5, 15),
                new BigDecimal("25000.00"),
                null
        ));
        String body = mockMvc.perform(withTenantAuth(tenant, post("/api/v1/invoices")
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

    private record InvoicePayload(
            UUID waterRightId,
            String invoiceNumber,
            LocalDate periodStart,
            LocalDate periodEnd,
            LocalDate issueDate,
            LocalDate dueDate,
            BigDecimal totalAmount,
            String notes
    ) {
    }
}
