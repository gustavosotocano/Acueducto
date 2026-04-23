package com.acueducto.veredal.interests.infrastructure.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "interest_tier")
public class InterestTierEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(name = "day_from", nullable = false)
    private int dayFrom;

    @Column(name = "day_to")
    private Integer dayTo;

    @Column(name = "rate_percent", nullable = false, precision = 6, scale = 3)
    private BigDecimal ratePercent;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;
}

