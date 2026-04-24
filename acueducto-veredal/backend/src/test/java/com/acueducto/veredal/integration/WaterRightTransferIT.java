package com.acueducto.veredal.integration;

import com.acueducto.veredal.support.IntegrationTestBase;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WaterRightTransferIT extends IntegrationTestBase {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void transferMaintainsSameWaterRightAndWritesOwnerHistory() throws Exception {
        String tenant = "tenant_transfer";

        UUID owner1Id = createOwner(tenant, "101", "Owner One");
        UUID owner2Id = createOwner(tenant, "202", "Owner Two");
        UUID propertyId = createProperty(tenant, owner1Id);
        UUID waterRightId = createWaterRight(tenant, owner1Id, propertyId);

        LocalDate transferDate = LocalDate.of(2026, 3, 18);
        String transferPayload = objectMapper.writeValueAsString(new TransferPayload(
                owner2Id,
                transferDate,
                "SALE",
                "test transfer"
        ));

        mockMvc.perform(post("/api/v1/water-rights/{id}/transfers", waterRightId)
                        .header("X-Tenant-Id", tenant)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(transferPayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(waterRightId.toString())))
                .andExpect(jsonPath("$.ownerId", is(owner2Id.toString())));

        mockMvc.perform(get("/api/v1/water-rights/{id}/owner-history", waterRightId)
                        .header("X-Tenant-Id", tenant))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].ownerId", is(owner2Id.toString())))
                .andExpect(jsonPath("$[0].endDate").value(nullValue()))
                .andExpect(jsonPath("$[1].ownerId", is(owner1Id.toString())));
    }

    @Test
    void deactivateRequiresOwnershipEndDate() throws Exception {
        String tenant = "tenant_deactivate";

        UUID ownerId = createOwner(tenant, "303", "Owner Three");
        UUID propertyId = createProperty(tenant, ownerId);
        UUID waterRightId = createWaterRight(tenant, ownerId, propertyId);

        mockMvc.perform(post("/api/v1/water-rights/{id}/deactivate", waterRightId)
                        .header("X-Tenant-Id", tenant)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    private UUID createOwner(String tenant, String documentNumber, String fullName) throws Exception {
        String payload = objectMapper.writeValueAsString(new OwnerPayload(
                "CC",
                documentNumber,
                fullName,
                "3000000000",
                fullName.toLowerCase().replace(" ", ".") + "@example.com"
        ));

        String body = mockMvc.perform(post("/api/v1/owners")
                        .header("X-Tenant-Id", tenant)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return UUID.fromString(objectMapper.readTree(body).get("id").asText());
    }

    private UUID createProperty(String tenant, UUID ownerId) throws Exception {
        String payload = objectMapper.writeValueAsString(new PropertyPayload(
                "P-" + ownerId.toString().substring(0, 8),
                "Predio test",
                "Calle 123",
                ownerId,
                LocalDate.of(2026, 1, 1)
        ));

        String body = mockMvc.perform(post("/api/v1/properties")
                        .header("X-Tenant-Id", tenant)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return UUID.fromString(objectMapper.readTree(body).get("id").asText());
    }

    private UUID createWaterRight(String tenant, UUID ownerId, UUID propertyId) throws Exception {
        String payload = objectMapper.writeValueAsString(new WaterRightPayload(
                "WR-" + ownerId.toString().substring(0, 8),
                ownerId,
                propertyId,
                null,
                "ACTIVE",
                "RESIDENTIAL",
                new BigDecimal("25000.00"),
                LocalDate.of(2026, 1, 1)
        ));

        String body = mockMvc.perform(post("/api/v1/water-rights")
                        .header("X-Tenant-Id", tenant)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(body);
        return UUID.fromString(json.get("id").asText());
    }

    private record OwnerPayload(
            String documentType,
            String documentNumber,
            String fullName,
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

    private record TransferPayload(
            UUID newOwnerId,
            LocalDate transferDate,
            String transferReason,
            String notes
    ) {
    }
}

