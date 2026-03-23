package com.acueducto.veredal.properties.infrastructure.persistence.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@Entity
@Table(name = "property")
public class PropertyEntity {

    @Id
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "tenant_id", nullable = false, length = 64)
    private String tenantId;

    @Column(name = "property_code", nullable = false, length = 50)
    private String propertyCode;

    @Column(name = "property_name", nullable = false, length = 200)
    private String propertyName;

    @Column(name = "address_line", nullable = false, length = 250)
    private String addressLine;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;
}
