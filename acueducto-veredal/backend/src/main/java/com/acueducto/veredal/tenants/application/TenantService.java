package com.acueducto.veredal.tenants.application;

import com.acueducto.veredal.auth.application.UserService;
import com.acueducto.veredal.shared.security.SecurityUtils;
import com.acueducto.veredal.tenants.infrastructure.persistence.jpa.TenantEntity;
import com.acueducto.veredal.tenants.infrastructure.persistence.jpa.TenantRepository;
import com.acueducto.veredal.tenants.interfaces.rest.controller.CreateTenantRequest;
import com.acueducto.veredal.tenants.interfaces.rest.controller.InitialPresidentRequest;
import com.acueducto.veredal.tenants.interfaces.rest.controller.UpdateTenantRequest;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@Transactional
public class TenantService {

    private final TenantRepository repository;
    private final UserService userService;

    public TenantService(TenantRepository repository, UserService userService) {
        this.repository = repository;
        this.userService = userService;
    }

    public TenantEntity create(CreateTenantRequest request) {
        if (!SecurityUtils.currentUser().isPlatformAdmin()) {
            throw new AccessDeniedException("Only platform administrators can create tenants");
        }

        TenantEntity tenant = persistNewTenant(request);
        InitialPresidentRequest president = request.president();
        userService.create(new UserService.CreateUserCommand(
                president.fullName(),
                president.email(),
                president.password(),
                "PRESIDENT",
                tenant.getId()
        ));
        return tenant;
    }

    @Transactional(readOnly = true)
    public List<TenantEntity> findAll() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public TenantEntity findById(String id) {
        return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Tenant not found"));
    }

    public TenantEntity update(String id, UpdateTenantRequest request) {
        TenantEntity t = repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Tenant not found"));
        t.setTenantName(request.tenantName().trim());
        t.setContactName(trimToNull(request.contactName()));
        t.setContactPhone(trimToNull(request.contactPhone()));
        t.setContactEmail(trimToNull(request.contactEmail()));
        t.setUpdatedAt(OffsetDateTime.now());
        return repository.save(t);
    }

    public TenantEntity deactivate(String id) {
        TenantEntity t = repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Tenant not found"));
        t.setStatus("INACTIVE");
        t.setUpdatedAt(OffsetDateTime.now());
        return repository.save(t);
    }

    private TenantEntity persistNewTenant(CreateTenantRequest request) {
        String id = request.id().trim();
        if (repository.existsById(id)) {
            throw new IllegalArgumentException("Tenant already exists");
        }

        OffsetDateTime now = OffsetDateTime.now();
        TenantEntity t = new TenantEntity();
        t.setId(id);
        t.setTenantName(request.tenantName().trim());
        t.setContactName(trimToNull(request.contactName()));
        t.setContactPhone(trimToNull(request.contactPhone()));
        t.setContactEmail(trimToNull(request.contactEmail()));
        t.setStatus("ACTIVE");
        t.setCreatedAt(now);
        t.setUpdatedAt(now);
        return repository.save(t);
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String v = s.trim();
        return v.isEmpty() ? null : v;
    }
}
