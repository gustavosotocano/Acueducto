package com.acueducto.veredal.integration;

import com.acueducto.veredal.support.IntegrationTestBase;
import com.acueducto.veredal.tenants.interfaces.rest.controller.CreateTenantRequest;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
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
    @Test
    void returnsTenantRequiredErrorWhenHeaderMissing() throws Exception {
        mockMvc.perform(withPlatformAuth(get("/api/v1/owners")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(startsWith("TENANT_REQUIRED")));
    }

    @Test
    void isolatesOwnersByTenant() throws Exception {
        createTenant("tenant_a");
        createTenant("tenant_b");
        String ownerPayload = objectMapper.writeValueAsString(new CreateOwnerPayload(
                "CC",
                "123",
                "Juan Perez",
                "3000000000",
                "juan@example.com"
        ));

        mockMvc.perform(withTenantAuth("tenant_a", post("/api/v1/owners")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ownerPayload)))
                .andExpect(status().isCreated());

        mockMvc.perform(withTenantAuth("tenant_a", get("/api/v1/owners")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));

        mockMvc.perform(withTenantAuth("tenant_b", get("/api/v1/owners")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void preventsCrossTenantReadAndWriteById() throws Exception {
        String tenantA = "tenant_a2";
        String tenantB = "tenant_b2";
        createTenant(tenantA);
        createTenant(tenantB);

        String ownerPayload = objectMapper.writeValueAsString(new CreateOwnerPayload(
                "CC",
                "999",
                "Owner A",
                "3000000000",
                "owner.a@example.com"
        ));

        String createdBody = mockMvc.perform(withTenantAuth(tenantA, post("/api/v1/owners")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ownerPayload)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String ownerId = objectMapper.readTree(createdBody).get("id").asText();

        mockMvc.perform(withTenantAuth(tenantB, get("/api/v1/owners/{id}", ownerId)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Owner not found")));

        String updatePayload = objectMapper.writeValueAsString(new UpdateOwnerPayload(
                "Owner A Updated",
                "3111111111",
                "owner.updated@example.com"
        ));

        mockMvc.perform(withTenantAuth(tenantB, put("/api/v1/owners/{id}", ownerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updatePayload)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", is("Owner not found")));

        mockMvc.perform(withTenantAuth(tenantB, post("/api/v1/owners/{id}/deactivate", ownerId)))
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
}

