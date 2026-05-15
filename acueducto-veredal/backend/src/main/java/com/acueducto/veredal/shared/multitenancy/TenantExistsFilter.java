package com.acueducto.veredal.shared.multitenancy;

import com.acueducto.veredal.tenants.infrastructure.persistence.jpa.TenantRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class TenantExistsFilter extends OncePerRequestFilter {

    private final TenantRepository tenantRepository;

    public TenantExistsFilter(TenantRepository tenantRepository) {
        this.tenantRepository = tenantRepository;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        // Tenant administration endpoints should be accessible without an existing tenant header.
        if (path.startsWith("/api/v1/tenants")) return true;
        if (path.startsWith("/api/v1/auth")) return true;
        if (path.startsWith("/actuator")) return true;
        return false;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        TenantContext.getTenantId().ifPresent((tenantId) -> {
            boolean exists = tenantRepository.findByIdAndStatus(tenantId, "ACTIVE").isPresent();
            if (!exists) throw new TenantNotFoundException(tenantId);
        });

        filterChain.doFilter(request, response);
    }
}

