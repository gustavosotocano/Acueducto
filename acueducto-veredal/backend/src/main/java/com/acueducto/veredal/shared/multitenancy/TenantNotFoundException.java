package com.acueducto.veredal.shared.multitenancy;

public class TenantNotFoundException extends IllegalArgumentException {
    public TenantNotFoundException(String tenantId) {
        super("Tenant not found: " + tenantId);
    }
}

