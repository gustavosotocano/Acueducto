package com.acueducto.veredal.support;

import com.acueducto.veredal.auth.application.AuthDataInitializer;
import com.acueducto.veredal.tenants.interfaces.rest.controller.CreateTenantRequest;
import com.acueducto.veredal.tenants.interfaces.rest.controller.InitialPresidentRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.testcontainers.containers.PostgreSQLContainer;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * One PostgreSQL container for the whole test JVM so Spring contexts do not keep
 * a JDBC URL pointing at a container that was already stopped by another test class.
 */
public abstract class IntegrationTestBase {

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @SuppressWarnings("resource")
    private static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("acueducto_test")
            .withUsername("acueducto_user")
            .withPassword("acueducto_pass");

    static {
        POSTGRES.start();
    }

    protected MockHttpServletRequestBuilder withTenantAuth(String tenantId, MockHttpServletRequestBuilder builder)
            throws Exception {
        return AuthTestSupport.withTenantAuth(mockMvc, objectMapper, tenantId, builder);
    }

    protected MockHttpServletRequestBuilder withPlatformAuth(MockHttpServletRequestBuilder builder) throws Exception {
        return builder.header("Authorization", "Bearer " + AuthTestSupport.adminToken(mockMvc, objectMapper));
    }

    protected void createTenant(String id) throws Exception {
        createTenant(id, "president." + id + "@test.local", "Presidente " + id);
    }

    protected void createTenant(String id, String presidentEmail, String presidentFullName) throws Exception {
        String body = objectMapper.writeValueAsString(new CreateTenantRequest(
                id,
                "Tenant " + id,
                null,
                null,
                null,
                new InitialPresidentRequest(
                        presidentFullName,
                        presidentEmail,
                        AuthDataInitializer.DEFAULT_PASSWORD
                )
        ));
        mockMvc.perform(withPlatformAuth(post("/api/v1/tenants"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated());
    }

    @DynamicPropertySource
    static void registerDataSourceProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", POSTGRES::getDriverClassName);

        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");

        registry.add("spring.flyway.enabled", () -> "true");
        registry.add("spring.flyway.locations", () -> "classpath:db/migration");
    }
}
