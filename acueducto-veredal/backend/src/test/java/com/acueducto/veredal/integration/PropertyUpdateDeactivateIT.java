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

import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PropertyUpdateDeactivateIT extends IntegrationTestBase {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void updateAndDeactivateProperty() throws Exception {
        String tenant = "tenant_prop_update";

        UUID ownerId = createOwner(tenant, "701", "Owner Prop");
        UUID propertyId = createProperty(tenant, ownerId, LocalDate.of(2026, 1, 1));

        String updatePayload = objectMapper.writeValueAsString(new UpdatePropertyPayload(
                "Predio actualizado",
                "Carrera 45 # 10-20"
        ));

        mockMvc.perform(put("/api/v1/properties/{id}", propertyId)
                        .header("X-Tenant-Id", tenant)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(propertyId.toString())))
                .andExpect(jsonPath("$.propertyName", is("Predio actualizado")))
                .andExpect(jsonPath("$.addressLine", is("Carrera 45 # 10-20")));

        mockMvc.perform(post("/api/v1/properties/{id}/deactivate", propertyId)
                        .header("X-Tenant-Id", tenant))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id", is(propertyId.toString())))
                .andExpect(jsonPath("$.status", is("INACTIVE")));

        mockMvc.perform(get("/api/v1/properties/{id}", propertyId)
                        .header("X-Tenant-Id", tenant))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("INACTIVE")));
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

    private UUID createProperty(String tenant, UUID ownerId, LocalDate ownershipStartDate) throws Exception {
        String payload = objectMapper.writeValueAsString(new CreatePropertyPayload(
                "P-" + ownerId.toString().substring(0, 8),
                "Predio base",
                "Calle 123",
                ownerId,
                ownershipStartDate
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
}

