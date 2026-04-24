package com.acueducto.veredal.shared.multitenancy;

public class TenantRequiredException extends IllegalStateException {
    public TenantRequiredException() {
        super("Tenant id is required");
    }
}

