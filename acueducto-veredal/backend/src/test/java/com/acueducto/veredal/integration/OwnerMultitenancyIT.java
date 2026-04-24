package com.acueducto.veredal.integration;

import com.acueducto.veredal.support.IntegrationTestBase;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.ActiveProfiles;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.startsWith;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class OwnerMultitenancyIT extends IntegrationTestBase {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Test
    void returnsTenantRequiredErrorWhenHeaderMissing() throws Exception {
        mockMvc.perform(get("/api/v1/owners"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(startsWith("TENANT_REQUIRED")));
    }

    @Test
    void isolatesOwnersByTenant() throws Exception {
        String ownerPayload = objectMapper.writeValueAsString(new CreateOwnerPayload(
                "CC",
                "123",
                "Juan Perez",
                "3000000000",
                "juan@example.com"
        ));

        mockMvc.perform(post("/api/v1/owners")
                        .header("X-Tenant-Id", "tenant_a")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ownerPayload))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/v1/owners")
                        .header("X-Tenant-Id", "tenant_a"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(get("/api/v1/owners")
                        .header("X-Tenant-Id", "tenant_b"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void preventsCrossTenantReadAndWriteById() throws Exception {
        String tenantA = "tenant_a2";
        String tenantB = "tenant_b2";

        String ownerPayload = objectMapper.writeValueAsString(new CreateOwnerPayload(
                "CC",
                "999",
                "Owner A",
                "3000000000",
                "owner.a@example.com"
        ));

        String createdBody = mockMvc.perform(post("/api/v1/owners")
                        .header("X-Tenant-Id", tenantA)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ownerPayload))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String ownerId = objectMapper.readTree(createdBody).get("id").asText();

        mockMvc.perform(get("/api/v1/owners/{id}", ownerId)
                        .header("X-Tenant-Id", tenantB))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Owner not found")));

        String updatePayload = objectMapper.writeValueAsString(new UpdateOwnerPayload(
                "Owner A Updated",
                "3111111111",
                "owner.updated@example.com"
        ));

        mockMvc.perform(put("/api/v1/owners/{id}", ownerId)
                        .header("X-Tenant-Id", tenantB)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Owner not found")));

        mockMvc.perform(post("/api/v1/owners/{id}/deactivate", ownerId)
                        .header("X-Tenant-Id", tenantB))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Owner not found")));
    }

    private record CreateOwnerPayload(
            String documentType,
            String documentNumber,
            String fullName,
            String phone,
            String email
    ) {
    }

    private record UpdateOwnerPayload(
            String fullName,
            String phone,
            String email
    ) {
    }
}

