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
class WaterRightUpdateDeactivateIT extends IntegrationTestBase {
    @Test
    void updateAndDeactivateWaterRight() throws Exception {
        String tenant = "tenant_wr_update";

        UUID ownerId = createOwner(tenant, "801", "Owner WR");
        UUID property1Id = createProperty(tenant, ownerId, "P1-" + ownerId.toString().substring(0, 6));
        UUID property2Id = createProperty(tenant, ownerId, "P2-" + ownerId.toString().substring(0, 6));

        UUID waterRightId = createWaterRight(tenant, ownerId, property1Id, "WR-" + ownerId.toString().substring(0, 6));

        String updatePayload = objectMapper.writeValueAsString(new UpdateWaterRightPayload(
                property2Id,
                null,
                "ACTIVE",
                "COMMERCIAL",
                new BigDecimal("30000.00")
        ));

        mockMvc.perform(withTenantAuth(tenant, put("/api/v1/water-rights/{id}", waterRightId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(waterRightId.toString())))
                .andExpect(jsonPath("$.propertyId", is(property2Id.toString())))
                .andExpect(jsonPath("$.connectionType", is("COMMERCIAL")))
                .andExpect(jsonPath("$.baseTariff", is(30000.00)));

        LocalDate endDate = LocalDate.of(2026, 4, 23);
        String deactivatePayload = objectMapper.writeValueAsString(new DeactivateWaterRightPayload(endDate));

        mockMvc.perform(withTenantAuth(tenant, post("/api/v1/water-rights/{id}/deactivate", waterRightId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(deactivatePayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(waterRightId.toString())))
                .andExpect(jsonPath("$.serviceStatus", is("INACTIVE")))
                .andExpect(jsonPath("$.ownershipEndDate", is(endDate.toString())));

        mockMvc.perform(withTenantAuth(tenant, get("/api/v1/water-rights/{id}", waterRightId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.serviceStatus", is("INACTIVE")))
                .andExpect(jsonPath("$.ownershipEndDate", is(endDate.toString())));
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

    private UUID createProperty(String tenant, UUID ownerId, String code) throws Exception {
        String payload = objectMapper.writeValueAsString(new CreatePropertyPayload(
                code,
                "Predio " + code,
                "Direccion " + code,
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
}

