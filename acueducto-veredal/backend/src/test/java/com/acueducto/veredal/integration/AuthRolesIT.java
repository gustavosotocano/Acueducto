package com.acueducto.veredal.integration;

import com.acueducto.veredal.auth.application.AuthDataInitializer;
import com.acueducto.veredal.support.IntegrationTestBase;
import com.acueducto.veredal.tenants.interfaces.rest.controller.CreateTenantRequest;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthRolesIT extends IntegrationTestBase {

    private static final String TENANT = "tenant_rf066_roles";

    @BeforeEach
    void setUp() throws Exception {
        createTenant(TENANT);
        seedUser("presidente@demo.local", "PRESIDENT");
        seedUser("tesoreria@demo.local", "TREASURY");
    }

    @Test
    void presidentCannotRegisterPayments() throws Exception {
        String token = login("presidente@demo.local");

        mockMvc.perform(post("/api/v1/payments")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "paymentDate":"2026-05-01",
                                  "amount":1000.00,
                                  "method":"CASH"
                                }
                                """))
                .andExpect(status().isForbidden());
    }

    @Test
    void treasurerCanRegisterPaymentsWithoutInvoiceFailsValidation() throws Exception {
        String token = login("tesoreria@demo.local");

        mockMvc.perform(post("/api/v1/payments")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", TENANT)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "paymentDate":"2026-05-01",
                                  "amount":1000.00,
                                  "method":"CASH"
                                }
                                """))
                .andExpect(status().isBadRequest());
    }

    @Test
    void nonAdminCannotCreateTenants() throws Exception {
        String token = login("tesoreria@demo.local");
        String body = objectMapper.writeValueAsString(new CreateTenantRequest(
                "tenant_blocked",
                "Blocked",
                null,
                null,
                null
        ));

        mockMvc.perform(post("/api/v1/tenants")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isForbidden());
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

    private void seedUser(String email, String role) throws Exception {
        String payload = """
                {
                  "fullName":"Usuario %s",
                  "email":"%s",
                  "password":"%s",
                  "roleCode":"%s",
                  "tenantId":"%s"
                }
                """.formatted(role, email, AuthDataInitializer.DEFAULT_PASSWORD, role, TENANT);

        mockMvc.perform(withTenantAuth(TENANT, post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload)))
                .andExpect(status().isCreated());
    }

    private String login(String email) throws Exception {
        String payload = """
                {
                  "email":"%s",
                  "password":"%s"
                }
                """.formatted(email, AuthDataInitializer.DEFAULT_PASSWORD);

        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(response);
        return json.get("accessToken").asText();
    }
}
