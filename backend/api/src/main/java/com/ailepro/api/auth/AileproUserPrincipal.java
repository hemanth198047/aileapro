package com.ailepro.api.auth;

import com.ailepro.api.UserRoleRepository;
import com.ailepro.persistence.UserEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.UUID;

public class AileproUserPrincipal implements UserDetails {
    private final UserEntity user;
    private final Collection<? extends GrantedAuthority> authorities;

    public AileproUserPrincipal(UserEntity user, Collection<? extends GrantedAuthority> authorities) {
        this.user = user;
        this.authorities = authorities;
    }

    public static AileproUserPrincipal from(UserEntity user, UserRoleRepository roles) {
        var authorities = roles.findByUserId(user.getId()).stream()
                .map(r -> new SimpleGrantedAuthority("ROLE_" + r.getRoleCode().toUpperCase()))
                .toList();
        return new AileproUserPrincipal(user, authorities);
    }

    public UUID getId() {
        return user.getId();
    }

    public String getEmail() {
        return user.getEmail();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return user.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return "active".equals(user.getStatus());
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return "active".equals(user.getStatus());
    }
}
