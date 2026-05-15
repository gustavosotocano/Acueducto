package com.acueducto.veredal.integration;

import com.acueducto.veredal.support.IntegrationTestBase;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CrossTenantAccessIT extends IntegrationTestBase {
    @Test
    void preventsCrossTenantAccessForProperty() throws Exception {
        String tenantA = "tenant_prop_a";
        String tenantB = "tenant_prop_b";

        UUID ownerA = createOwner(tenantA, "901", "Owner A");
        UUID propertyId = createProperty(tenantA, ownerA, "P-CA002-1");

        mockMvc.perform(withTenantAuth(tenantB, get("/api/v1/properties/{id}", propertyId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Property not found")));

        String updatePayload = objectMapper.writeValueAsString(new UpdatePropertyPayload("X", "Y"));
        mockMvc.perform(withTenantAuth(tenantB, put("/api/v1/properties/{id}", propertyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Property not found")));

        mockMvc.perform(withTenantAuth(tenantB, post("/api/v1/properties/{id}/deactivate", propertyId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Property not found")));

        UUID ownerB = createOwner(tenantB, "902", "Owner B");
        String changeOwnerPayload = objectMapper.writeValueAsString(new ChangeOwnerPayload(
                ownerB, LocalDate.of(2026, 3, 10), "SALE", "x"
        ));
        mockMvc.perform(withTenantAuth(tenantB, post("/api/v1/properties/{id}/owner-changes", propertyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(changeOwnerPayload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Property not found")));

        mockMvc.perform(withTenantAuth(tenantB, get("/api/v1/properties/{id}/owner-history", propertyId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Property not found")));
    }

    @Test
    void preventsCrossTenantAccessForWaterRight() throws Exception {
        String tenantA = "tenant_wr_a";
        String tenantB = "tenant_wr_b";

        UUID ownerA = createOwner(tenantA, "903", "Owner A");
        UUID propertyA = createProperty(tenantA, ownerA, "P-CA002-2");
        UUID waterRightId = createWaterRight(tenantA, ownerA, propertyA, "WR-CA002-1");

        mockMvc.perform(withTenantAuth(tenantB, get("/api/v1/water-rights/{id}", waterRightId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Water right not found")));

        UUID ownerB = createOwner(tenantB, "904", "Owner B");
        UUID propertyB = createProperty(tenantB, ownerB, "P-CA002-3");

        String updatePayload = objectMapper.writeValueAsString(new UpdateWaterRightPayload(
                propertyB, null, "ACTIVE", "COMMERCIAL", new BigDecimal("1.00")
        ));
        mockMvc.perform(withTenantAuth(tenantB, put("/api/v1/water-rights/{id}", waterRightId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Water right not found")));

        String deactivatePayload = objectMapper.writeValueAsString(new DeactivateWaterRightPayload(LocalDate.of(2026, 4, 23)));
        mockMvc.perform(withTenantAuth(tenantB, post("/api/v1/water-rights/{id}/deactivate", waterRightId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deactivatePayload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Water right not found")));

        String transferPayload = objectMapper.writeValueAsString(new TransferPayload(
                ownerB, LocalDate.of(2026, 3, 18), "SALE", "x"
        ));
        mockMvc.perform(withTenantAuth(tenantB, post("/api/v1/water-rights/{id}/transfers", waterRightId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferPayload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Water right not found")));

        mockMvc.perform(withTenantAuth(tenantB, get("/api/v1/water-rights/{id}/owner-history", waterRightId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Water right not found")));
    }

    private UUID createOwner(String tenant, String documentNumber, String fullName) throws Exception {
        String payload = objectMapper.writeValueAsString(new OwnerPayload(
                "CC",
                documentNumber,
                fullName,
                "3000000000",
                fullName.toLowerCase().replace(" ", ".") + "@example.com"
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

    private UUID createProperty(String tenant, UUID ownerId, String propertyCode) throws Exception {
        String payload = objectMapper.writeValueAsString(new CreatePropertyPayload(
                propertyCode,
                "Predio " + propertyCode,
                "Direccion " + propertyCode,
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
        String payload = objectMapper.writeValueAsString(new CreateWaterRightPayload(
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
            String fullName,
            String phone,
            String email
    ) {
    }

    private record CreatePropertyPayload(
            String propertyCode,
            String propertyName,
            String addressLine,
            UUID ownerId,
            LocalDate ownershipStartDate
    ) {
    }

    private record UpdatePropertyPayload(
            String propertyName,
            String addressLine
    ) {
    }

    private record ChangeOwnerPayload(
            UUID newOwnerId,
            LocalDate changeDate,
            String changeReason,
            String notes
    ) {
    }

    private record CreateWaterRightPayload(
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

    private record UpdateWaterRightPayload(
            UUID propertyId,
            UUID waterMeterId,
            String serviceStatus,
            String connectionType,
            BigDecimal baseTariff
    ) {
    }

    private record DeactivateWaterRightPayload(LocalDate ownershipEndDate) {
    }

    private record TransferPayload(
            UUID newOwnerId,
            LocalDate transferDate,
            String transferReason,
            String notes
    ) {
    }
}

