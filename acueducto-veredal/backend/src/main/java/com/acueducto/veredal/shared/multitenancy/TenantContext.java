package com.acueducto.veredal.shared.multitenancy;

import java.util.Optional;

public final class TenantContext {

    private static final ScopedValue<String> CURRENT_TENANT = ScopedValue.newInstance();

    private TenantContext() {
    }

    public static Optional<String> getTenantId() {
        return CURRENT_TENANT.isBound()
                ? Optional.of(CURRENT_TENANT.get())
                : Optional.empty();
    }

    public static String getRequiredTenantId() {
        if (!CURRENT_TENANT.isBound()) {
            throw new TenantRequiredException();
        }
        return CURRENT_TENANT.get();
    }

    /**
     * Binds {@code tenantId} for the duration of {@code action} (e.g. servlet filter chain).
     */
    public static void runWithTenant(String tenantId, TenantScopedAction action) throws Exception {
        ScopedValue.where(CURRENT_TENANT, tenantId).call(() -> {
            action.run();
            return null;
        });
    }

    @FunctionalInterface
    public interface TenantScopedAction {
        void run() throws Exception;
    }
}
