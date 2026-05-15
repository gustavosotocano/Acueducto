package com.acueducto.veredal.support;

import com.acueducto.veredal.auth.application.AuthDataInitializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public final class AuthTestSupport {

    private static volatile String cachedAdminToken;

    private AuthTestSupport() {
    }

    public static String adminToken(MockMvc mockMvc, ObjectMapper objectMapper) throws Exception {
        if (cachedAdminToken != null) {
            return cachedAdminToken;
        }
        String payload = """
                {
                  "email":"%s",
                  "password":"%s"
                }
                """.formatted(AuthDataInitializer.PLATFORM_ADMIN_EMAIL, AuthDataInitializer.DEFAULT_PASSWORD);

        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode json = objectMapper.readTree(response);
        cachedAdminToken = json.get("accessToken").asText();
        return cachedAdminToken;
    }

    public static MockHttpServletRequestBuilder withTenantAuth(
            MockMvc mockMvc,
            ObjectMapper objectMapper,
            String tenantId,
            MockHttpServletRequestBuilder builder
    ) throws Exception {
        return builder
                .header("Authorization", "Bearer " + adminToken(mockMvc, objectMapper))
                .header("X-Tenant-Id", tenantId);
    }
}
