package com.acueducto.veredal.owners.infrastructure.persistence.jpa;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OwnerRepository extends JpaRepository<OwnerEntity, UUID> {
    List<OwnerEntity> findAllByTenantIdOrderByFullNameAsc(String tenantId);
    Optional<OwnerEntity> findByIdAndTenantId(UUID id, String tenantId);
    boolean existsByTenantIdAndDocumentTypeAndDocumentNumber(String tenantId, String documentType, String documentNumber);
}
