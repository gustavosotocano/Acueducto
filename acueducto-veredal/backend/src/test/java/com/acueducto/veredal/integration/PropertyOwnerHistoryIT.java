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
class PropertyOwnerHistoryIT extends IntegrationTestBase {
    @Test
    void changeOwnerClosesPreviousHistoryAndOpensNewOne() throws Exception {
        String tenant = "tenant_property_history";

        UUID owner1Id = createOwner(tenant, "501", "Owner One");
        UUID owner2Id = createOwner(tenant, "502", "Owner Two");

        LocalDate ownershipStartDate = LocalDate.of(2026, 1, 1);
        UUID propertyId = createProperty(tenant, owner1Id, ownershipStartDate);

        mockMvc.perform(withTenantAuth(tenant, get("/api/v1/properties/{id}/owner-history", propertyId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].ownerId", is(owner1Id.toString())))
                .andExpect(jsonPath("$[0].startDate", is(ownershipStartDate.toString())))
                .andExpect(jsonPath("$[0].endDate").value(nullValue()));

        LocalDate changeDate = LocalDate.of(2026, 3, 10);
        String changePayload = objectMapper.writeValueAsString(new ChangeOwnerPayload(
                owner2Id,
                changeDate,
                "SALE",
                "Cambio de propietario por compraventa"
        ));

        mockMvc.perform(withTenantAuth(tenant, post("/api/v1/properties/{id}/owner-changes", propertyId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(changePayload)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ownerId", is(owner2Id.toString())));

        mockMvc.perform(withTenantAuth(tenant, get("/api/v1/properties/{id}/owner-history", propertyId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2)))
                // newest record first
                .andExpect(jsonPath("$[0].ownerId", is(owner2Id.toString())))
                .andExpect(jsonPath("$[0].startDate", is(changeDate.toString())))
                .andExpect(jsonPath("$[0].endDate").value(nullValue()))
                // previous record closed at changeDate - 1
                .andExpect(jsonPath("$[1].ownerId", is(owner1Id.toString())))
                .andExpect(jsonPath("$[1].startDate", is(ownershipStartDate.toString())))
                .andExpect(jsonPath("$[1].endDate", is(changeDate.minusDays(1).toString())));
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

    private UUID createProperty(String tenant, UUID ownerId, LocalDate ownershipStartDate) throws Exception {
        String payload = objectMapper.writeValueAsString(new PropertyPayload(
                "P-" + ownerId.toString().substring(0, 8),
                "Predio test",
                "Calle 123",
                ownerId,
                ownershipStartDate
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

    private record ChangeOwnerPayload(
            UUID newOwnerId,
            LocalDate changeDate,
            String changeReason,
            String notes
    ) {
    }
}

