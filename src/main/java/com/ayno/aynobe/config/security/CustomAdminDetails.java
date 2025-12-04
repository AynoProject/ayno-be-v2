package com.ayno.aynobe.config.security;

import com.ayno.aynobe.entity.Admin;
import com.ayno.aynobe.entity.enums.AdminStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.Collections;

@RequiredArgsConstructor
public class CustomAdminDetails implements UserDetails {
    private final Admin admin;

    @Override public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMIN"));
    }

    public Admin getAdmin() { return admin; }

    @Override public String getPassword() {
        return admin.getPasswordHash();

    }
    @Override public String getUsername() {
        return admin.getAdminName();
    }

    //기타
    @Override public boolean isAccountNonExpired() {
        return true;
    }

    @Override public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override public boolean isAccountNonLocked() {
        return admin.getStatus() == AdminStatus.ACTIVE;
    }

    @Override public boolean isEnabled() {
        return admin.getStatus() == AdminStatus.ACTIVE;
    }
}
