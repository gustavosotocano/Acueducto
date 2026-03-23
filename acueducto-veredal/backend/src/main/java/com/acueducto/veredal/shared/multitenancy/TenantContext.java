package com.acueducto.veredal.shared.multitenancy;

import java.util.Optional;

public final class TenantContext {

    private static final ThreadLocal<String> CURRENT_TENANT = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void setTenantId(String tenantId) {
        CURRENT_TENANT.set(tenantId);
    }

    public static Optional<String> getTenantId() {
        return Optional.ofNullable(CURRENT_TENANT.get());
    }

    public static String getRequiredTenantId() {
        return getTenantId().orElseThrow(() -> new IllegalStateException("Tenant id is required"));
    }

    public static void clear() {
        CURRENT_TENANT.remove();
    }
}
