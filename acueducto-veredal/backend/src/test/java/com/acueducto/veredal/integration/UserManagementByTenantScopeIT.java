package com.acueducto.veredal.integration;

import com.acueducto.veredal.auth.application.AuthDataInitializer;
import com.acueducto.veredal.support.IntegrationTestBase;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class UserManagementByTenantScopeIT extends IntegrationTestBase {

    private String tenantA;
    private String tenantB;
    private String presidentAEmail;
    private String presidentBEmail;
    private String treasuryAEmail;

    @BeforeEach
    void setUp() throws Exception {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        tenantA = "tenant_a_" + suffix;
        tenantB = "tenant_b_" + suffix;
        presidentAEmail = "president.a." + suffix + "@test.local";
        presidentBEmail = "president.b." + suffix + "@test.local";
        treasuryAEmail = "treasury.a." + suffix + "@test.local";

        createTenant(tenantA, presidentAEmail, "Presidente A");
        createTenant(tenantB, presidentBEmail, "Presidente B");
        seedTreasuryForTenantA();
    }

    @Test
    void presidentCreatesTreasuryUserInOwnTenant() throws Exception {
        String token = login(presidentAEmail);
        String email = "new.treasury." + tenantA + "@test.local";

        String response = mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createUserPayload("Nuevo tesorero", email, "TREASURY", null)))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(response);
        assertThat(json.get("tenantId").asText()).isEqualTo(tenantA);
        assertThat(json.get("roleCode").asText()).isEqualTo("TREASURY");
    }

    @Test
    void presidentCannotAssignAnotherTenantInBody() throws Exception {
        String token = login(presidentAEmail);
        String email = "blocked." + tenantA + "@test.local";

        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createUserPayload("Bloqueado", email, "TREASURY", tenantB)))
                .andExpect(status().isForbidden());
    }

    @Test
    void presidentCannotCreatePresidentRole() throws Exception {
        String token = login(presidentAEmail);
        String email = "president2." + tenantA + "@test.local";

        mockMvc.perform(post("/api/v1/users")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createUserPayload("Otro presidente", email, "PRESIDENT", null)))
                .andExpect(status().isForbidden());
    }

    @Test
    void treasurerCannotAccessUsersApi() throws Exception {
        String token = login(treasuryAEmail);

        mockMvc.perform(get("/api/v1/users")
                        .header("Authorization", "Bearer " + token)
                        .header("X-Tenant-Id", tenantA))
                .andExpect(status().isForbidden());
    }

    @Test
    void platformAdminCreatesUsersInDifferentTenantsWithoutTenantHeader() throws Exception {
        String emailA = "platform.treasury.a." + tenantA + "@test.local";
        String emailB = "platform.treasury.b." + tenantB + "@test.local";

        mockMvc.perform(withPlatformAuth(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createUserPayload("Tesorero A", emailA, "TREASURY", tenantA))))
                .andExpect(status().isCreated());

        mockMvc.perform(withPlatformAuth(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createUserPayload("Tesorero B", emailB, "TREASURY", tenantB))))
                .andExpect(status().isCreated());

        String listResponse = mockMvc.perform(withPlatformAuth(get("/api/v1/users")))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode users = objectMapper.readTree(listResponse);
        assertThat(users.isArray()).isTrue();
        assertThat(users.findValuesAsText("email")).contains(emailA, emailB);
    }

    @Test
    void platformAdminListsUsersFilteredByTenant() throws Exception {
        String email = "filtered." + tenantA + "@test.local";
        mockMvc.perform(withPlatformAuth(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createUserPayload("Filtrado", email, "TREASURY", tenantA))))
                .andExpect(status().isCreated());

        String listResponse = mockMvc.perform(withPlatformAuth(get("/api/v1/users"))
                        .param("tenantId", tenantA))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode users = objectMapper.readTree(listResponse);
        assertThat(users.findValuesAsText("email")).contains(email);
        for (JsonNode user : users) {
            assertThat(user.get("tenantId").asText()).isEqualTo(tenantA);
        }
    }

    @Test
    void platformAdminCannotCreatePlatformAdminViaUsersEndpoint() throws Exception {
        String email = "new.admin." + UUID.randomUUID() + "@test.local";

        mockMvc.perform(withPlatformAuth(post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createUserPayload("Admin", email, "ADMIN", null))))
                .andExpect(status().isForbidden());
    }

    private void seedTreasuryForTenantA() throws Exception {
        mockMvc.perform(withTenantAuth(tenantA, post("/api/v1/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(createUserPayload(
                                "Tesorero A",
                                treasuryAEmail,
                                "TREASURY",
                                tenantA))))
                .andExpect(status().isCreated());
    }

    private String createUserPayload(String fullName, String email, String roleCode, String tenantId) {
        String tenantField = tenantId == null ? "null" : "\"" + tenantId + "\"";
        return """
                {
                  "fullName":"%s",
                  "email":"%s",
                  "password":"%s",
                  "roleCode":"%s",
                  "tenantId":%s
                }
                """.formatted(fullName, email, AuthDataInitializer.DEFAULT_PASSWORD, roleCode, tenantField);
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

        return objectMapper.readTree(response).get("accessToken").asText();
    }
}
