package com.acueducto.veredal.waterrights.infrastructure.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "water_right")
public class WaterRightEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(name = "right_code", nullable = false, length = 50)
    private String rightCode;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "property_id", nullable = false)
    private UUID propertyId;

    @Column(name = "water_meter_id")
    private UUID waterMeterId;

    @Column(name = "service_status", nullable = false, length = 20)
    private String serviceStatus;

    @Column(name = "connection_type", nullable = false, length = 30)
    private String connectionType;

    @Column(name = "base_tariff", nullable = false, precision = 14, scale = 2)
    private BigDecimal baseTariff;

    @Column(name = "ownership_start_date", nullable = false)
    private LocalDate ownershipStartDate;

    @Column(name = "ownership_end_date")
    private LocalDate ownershipEndDate;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
