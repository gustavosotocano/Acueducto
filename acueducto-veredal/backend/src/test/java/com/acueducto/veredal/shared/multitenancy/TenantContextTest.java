package com.acueducto.veredal.shared.multitenancy;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TenantContextTest {

    @Test
    void getTenantId_isEmptyOutsideScope() {
        assertThat(TenantContext.getTenantId()).isEmpty();
    }

    @Test
    void getRequiredTenantId_throwsOutsideScope() {
        assertThatThrownBy(TenantContext::getRequiredTenantId)
                .isInstanceOf(TenantRequiredException.class);
    }

    @Test
    void runWithTenant_bindsForDurationOfAction() throws Exception {
        AtomicReference<String> inside = new AtomicReference<>();
        TenantContext.runWithTenant("demo", () -> inside.set(TenantContext.getRequiredTenantId()));

        assertThat(inside).hasValue("demo");
        assertThat(TenantContext.getTenantId()).isEmpty();
    }

    @Test
    void nestedRunWithTenant_innerScopeShadowsOuter() throws Exception {
        AtomicReference<String> captured = new AtomicReference<>();
        TenantContext.runWithTenant("outer", () ->
                TenantContext.runWithTenant("inner", () ->
                        captured.set(TenantContext.getRequiredTenantId())));

        assertThat(captured).hasValue("inner");
        assertThat(TenantContext.getTenantId()).isEmpty();
    }
}
