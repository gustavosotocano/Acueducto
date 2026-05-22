package com.acueducto.veredal.integration;

import com.acueducto.veredal.auth.application.AuthDataInitializer;
import com.acueducto.veredal.support.IntegrationTestBase;
import com.acueducto.veredal.tenants.interfaces.rest.controller.CreateTenantRequest;
import com.acueducto.veredal.tenants.interfaces.rest.controller.InitialPresidentRequest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TenantCreateWithPresidentIT extends IntegrationTestBase {

    @Test
    void createsTenantAndPresidentInOneRequest() throws Exception {
        String tenantId = "tenant_rf067_" + UUID.randomUUID().toString().substring(0, 8);
        String presidentEmail = "presidente." + tenantId + "@test.local";

        String body = objectMapper.writeValueAsString(new CreateTenantRequest(
                tenantId,
                "Acueducto RF-067",
                null,
                null,
                null,
                new InitialPresidentRequest("Presidente RF-067", presidentEmail, AuthDataInitializer.DEFAULT_PASSWORD)
        ));

        mockMvc.perform(withPlatformAuth(post("/api/v1/tenants"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(tenantId))
                .andExpect(jsonPath("$.status").value("ACTIVE"));

        String loginResponse = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email":"%s",
                                  "password":"%s"
                                }
                                """.formatted(presidentEmail, AuthDataInitializer.DEFAULT_PASSWORD)))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode profile = objectMapper.readTree(loginResponse).get("user");
        assertThat(profile.get("role").asText()).isEqualTo("PRESIDENT");
        assertThat(profile.get("tenantId").asText()).isEqualTo(tenantId);
    }

    @Test
    void duplicatePresidentEmailDoesNotPersistTenant() throws Exception {
        String tenantId = "tenant_rf067_dup_" + UUID.randomUUID().toString().substring(0, 8);
        String presidentEmail = "dup.presidente@rf067.test.local";

        createTenant(tenantId, presidentEmail, "Presidente A");

        String body = objectMapper.writeValueAsString(new CreateTenantRequest(
                tenantId + "_orphan",
                "Tenant huérfano",
                null,
                null,
                null,
                new InitialPresidentRequest("Presidente B", presidentEmail, AuthDataInitializer.DEFAULT_PASSWORD)
        ));

        mockMvc.perform(withPlatformAuth(post("/api/v1/tenants"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());

        mockMvc.perform(withPlatformAuth(get("/api/v1/tenants/{id}", tenantId + "_orphan")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Tenant not found"));
    }
}
