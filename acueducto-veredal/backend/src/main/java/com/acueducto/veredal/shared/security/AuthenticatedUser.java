package com.acueducto.veredal.shared.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class AuthenticatedUser implements UserDetails {

    private final UUID id;
    private final String email;
    private final String role;
    private final String tenantId;

    public AuthenticatedUser(UUID id, String email, String role, String tenantId) {
        this.id = id;
        this.email = email;
        this.role = role;
        this.tenantId = tenantId;
    }

    public UUID getId() {
        return id;
    }

    public String getRole() {
        return role;
    }

    public String getTenantId() {
        return tenantId;
    }

    public boolean isPlatformAdmin() {
        return tenantId == null && "ADMIN".equalsIgnoreCase(role);
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.toUpperCase()));
    }

    @Override
    public String getPassword() {
        return "";
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
