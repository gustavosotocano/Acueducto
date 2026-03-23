package com.acueducto.veredal.owners.application;

import com.acueducto.veredal.owners.infrastructure.persistence.jpa.OwnerEntity;
import com.acueducto.veredal.owners.infrastructure.persistence.jpa.OwnerRepository;
import com.acueducto.veredal.owners.interfaces.rest.controller.CreateOwnerRequest;
import com.acueducto.veredal.owners.interfaces.rest.controller.UpdateOwnerRequest;
import com.acueducto.veredal.shared.multitenancy.TenantContext;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class OwnerService {

    private final OwnerRepository ownerRepository;

    public OwnerService(OwnerRepository ownerRepository) {
        this.ownerRepository = ownerRepository;
    }

    public OwnerEntity create(CreateOwnerRequest request) {
        String tenantId = TenantContext.getRequiredTenantId();
        if (ownerRepository.existsByTenantIdAndDocumentTypeAndDocumentNumber(
                tenantId, request.documentType(), request.documentNumber())) {
            throw new IllegalArgumentException("Owner already exists for document in this tenant");
        }

        OffsetDateTime now = OffsetDateTime.now();
        OwnerEntity entity = new OwnerEntity();
        entity.setId(UUID.randomUUID());
        entity.setTenantId(tenantId);
        entity.setDocumentType(request.documentType());
        entity.setDocumentNumber(request.documentNumber());
        entity.setFullName(request.fullName());
        entity.setPhone(request.phone());
        entity.setEmail(request.email());
        entity.setStatus("ACTIVE");
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        return ownerRepository.save(entity);
    }

    @Transactional(readOnly = true)
    public List<OwnerEntity> findAll() {
        return ownerRepository.findAllByTenantIdOrderByFullNameAsc(TenantContext.getRequiredTenantId());
    }

    @Transactional(readOnly = true)
    public OwnerEntity findById(UUID id) {
        return ownerRepository.findByIdAndTenantId(id, TenantContext.getRequiredTenantId())
                .orElseThrow(() -> new IllegalArgumentException("Owner not found"));
    }

    public OwnerEntity update(UUID id, UpdateOwnerRequest request) {
        OwnerEntity entity = findById(id);
        entity.setFullName(request.fullName());
        entity.setPhone(request.phone());
        entity.setEmail(request.email());
        entity.setUpdatedAt(OffsetDateTime.now());
        return ownerRepository.save(entity);
    }

    public OwnerEntity deactivate(UUID id) {
        OwnerEntity entity = findById(id);
        entity.setStatus("INACTIVE");
        entity.setUpdatedAt(OffsetDateTime.now());
        return ownerRepository.save(entity);
    }
}
