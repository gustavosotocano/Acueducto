package com.acueducto.veredal.interests.interfaces.rest.controller;

import com.acueducto.veredal.interests.application.InterestPolicyService;
import com.acueducto.veredal.shared.multitenancy.TenantContext;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/interests/config")
public class InterestConfigController {

    private final InterestPolicyService interestPolicyService;

    public InterestConfigController(InterestPolicyService interestPolicyService) {
        this.interestPolicyService = interestPolicyService;
    }

    @GetMapping
    public InterestConfigResponse get() {
        return new InterestConfigResponse(TenantContext.getRequiredTenantId(), interestPolicyService.isEnabled());
    }

    @PutMapping
    public InterestConfigResponse update(@Valid @RequestBody UpdateInterestConfigRequest request) {
        boolean enabled = interestPolicyService.upsertEnabled(request.enabled()).isEnabled();
        return new InterestConfigResponse(TenantContext.getRequiredTenantId(), enabled);
    }
}

