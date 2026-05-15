package com.acueducto.veredal.shared.multitenancy;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.acueducto.veredal.shared.security.AuthenticatedUser;
import com.acueducto.veredal.shared.security.SecurityUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 15)
public class TenantHeaderFilter extends OncePerRequestFilter {

    private final String headerName;

    public TenantHeaderFilter(@Value("${app.multitenancy.header-name:X-Tenant-Id}") String headerName) {
        this.headerName = headerName;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        try {
            String headerTenant = request.getHeader(headerName);
            if (StringUtils.hasText(headerTenant)) {
                String normalized = headerTenant.trim();
                AuthenticatedUser user = SecurityUtils.currentUserOrNull();
                if (user != null && user.getTenantId() != null && !user.getTenantId().equals(normalized)) {
                    throw new IllegalArgumentException("Tenant header does not match authenticated user");
                }
                TenantContext.setTenantId(normalized);
            }
            filterChain.doFilter(request, response);
        } finally {
            TenantContext.clear();
        }
    }
}
