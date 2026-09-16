package com.ailepro.api.auth;

import com.ailepro.api.UserRoleRepository;
import com.ailepro.persistence.UserEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Held in the HTTP session, so it must stay serializable and free of JPA entities.
 * Credentials are never copied here; fresh user state is loaded per request by id.
 */
public class AileproUserPrincipal implements UserDetails, Serializable {

    private static final long serialVersionUID = 1L;

    private final UUID id;
    private final String email;
    private final String status;
    private final List<SimpleGrantedAuthority> authorities;

    public AileproUserPrincipal(UUID id, String email, String status, Collection<SimpleGrantedAuthority> authorities) {
        this.id = id;
        this.email = email;
        this.status = status;
        this.authorities = List.copyOf(authorities);
    }

    public static AileproUserPrincipal from(UserEntity user, UserRoleRepository roles) {
        var authorities = roles.findByUserId(user.getId()).stream()
                .map(r -> new SimpleGrantedAuthority("ROLE_" + r.getRoleCode().toUpperCase()))
                .toList();
        return new AileproUserPrincipal(user.getId(), user.getEmail(), user.getStatus(), authorities);
    }

    public UUID getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return null;
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
        return "active".equals(status);
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return "active".equals(status);
    }
}
