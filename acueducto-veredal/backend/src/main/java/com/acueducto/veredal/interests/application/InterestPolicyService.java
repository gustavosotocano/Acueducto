package com.acueducto.veredal.interests.application;

import com.acueducto.veredal.interests.infrastructure.persistence.jpa.InterestConfigEntity;
import com.acueducto.veredal.interests.infrastructure.persistence.jpa.InterestConfigRepository;
import com.acueducto.veredal.interests.infrastructure.persistence.jpa.InterestTierEntity;
import com.acueducto.veredal.interests.infrastructure.persistence.jpa.InterestTierRepository;
import com.acueducto.veredal.shared.multitenancy.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Service
@Transactional
public class InterestPolicyService {

    private final InterestConfigRepository interestConfigRepository;
    private final InterestTierRepository interestTierRepository;

    public InterestPolicyService(
            InterestConfigRepository interestConfigRepository,
            InterestTierRepository interestTierRepository
    ) {
        this.interestConfigRepository = interestConfigRepository;
        this.interestTierRepository = interestTierRepository;
    }

    @Transactional(readOnly = true)
    public boolean isEnabled() {
        String tenantId = TenantContext.getRequiredTenantId();
        return interestConfigRepository.findById(tenantId).map(InterestConfigEntity::isEnabled).orElse(false);
    }

    @Transactional(readOnly = true)
    public BigDecimal resolveRatePercent(int daysOverdue) {
        if (daysOverdue <= 0) return BigDecimal.ZERO;
        String tenantId = TenantContext.getRequiredTenantId();
        List<InterestTierEntity> tiers = interestTierRepository.findAllByTenantIdOrderByDayFromAsc(tenantId);
        for (InterestTierEntity tier : tiers) {
            boolean afterStart = daysOverdue >= tier.getDayFrom();
            boolean beforeEnd = tier.getDayTo() == null || daysOverdue <= tier.getDayTo();
            if (afterStart && beforeEnd) {
                return tier.getRatePercent();
            }
        }
        return BigDecimal.ZERO;
    }

    public InterestConfigEntity upsertEnabled(boolean enabled) {
        String tenantId = TenantContext.getRequiredTenantId();
        OffsetDateTime now = OffsetDateTime.now();
        InterestConfigEntity config = interestConfigRepository.findById(tenantId).orElseGet(() -> {
            InterestConfigEntity c = new InterestConfigEntity();
            c.setTenantId(tenantId);
            c.setCreatedAt(now);
            return c;
        });
        config.setEnabled(enabled);
        config.setUpdatedAt(now);
        return interestConfigRepository.save(config);
    }
}

