package com.acueducto.veredal.integration;

import com.acueducto.veredal.support.IntegrationTestBase;
import com.acueducto.veredal.tenants.interfaces.rest.controller.CreateTenantRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ConnectionTariffOverlapIT extends IntegrationTestBase {
    @Test
    void rejectsOverlappingTariffForSameConnectionType() throws Exception {
        String tenant = "tenant_rf065_tariff";
        createTenant(tenant);

        String first = """
                {"connectionType":"RESIDENTIAL","amount":100.00,"validFrom":"2026-01-01","validTo":"2026-06-30"}
                """;
        mockMvc.perform(withTenantAuth(tenant, post("/api/v1/connection-tariffs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(first)))
                .andExpect(status().isCreated());

        String overlapping = """
                {"connectionType":"RESIDENTIAL","amount":200.00,"validFrom":"2026-03-01","validTo":null}
                """;
        mockMvc.perform(withTenantAuth(tenant, post("/api/v1/connection-tariffs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(overlapping)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("overlap")));

        String adjacent = """
                {"connectionType":"RESIDENTIAL","amount":150.00,"validFrom":"2026-07-01","validTo":null}
                """;
        mockMvc.perform(withTenantAuth(tenant, post("/api/v1/connection-tariffs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(adjacent)))
                .andExpect(status().isCreated());
    }

    @Test
    void applicableReturnsTariffValidOnDate() throws Exception {
        String tenant = "tenant_rf065_applicable";
        createTenant(tenant);

        String body = """
                {"connectionType":"COMMERCIAL","amount":999.50,"validFrom":"2026-02-01","validTo":"2026-08-31"}
                """;
        mockMvc.perform(withTenantAuth(tenant, post("/api/v1/connection-tariffs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body)))
                .andExpect(status().isCreated());

        mockMvc.perform(withTenantAuth(tenant, get("/api/v1/connection-tariffs/applicable")
                        .param("connectionType", "COMMERCIAL")
                        .param("onDate", "2026-04-15")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.connectionType", is("COMMERCIAL")))
                .andExpect(jsonPath("$.amount", is(999.5)));

        mockMvc.perform(withTenantAuth(tenant, get("/api/v1/connection-tariffs/applicable")
                        .param("connectionType", "COMMERCIAL")
                        .param("onDate", "2026-12-01")))
                .andExpect(status().isNotFound());
    }

}
