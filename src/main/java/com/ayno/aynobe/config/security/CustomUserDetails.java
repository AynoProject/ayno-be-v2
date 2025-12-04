package com.ayno.aynobe.config.security;

import com.ayno.aynobe.entity.User;
import com.ayno.aynobe.entity.enums.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

@RequiredArgsConstructor
public class CustomUserDetails implements UserDetails, OAuth2User {
    // 우리가 만든 User 엔티티 감싸기
    private final User user;

    // 기본 권한: ROLE_USER (관리자는 별도 시큐리티 체인/토큰 사용 예정)
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return Collections.singletonList(new SimpleGrantedAuthority("ROLE_USER"));
    }

    public User getUser() {
        return user;
    }

    // 일반 로그인 관련
    @Override
    public String getPassword() {
        return user.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    // 소셜 로그인 관련
    @Override
    public String getName() {
        return user.getUsername();
    }

    @Override public Map<String, Object> getAttributes() {
        return Map.of();
    }

    @Override public <A> A getAttribute(String name) {
        return null;
    }

    // 기타
    @Override
    public boolean isAccountNonExpired() {
        return true;  // 계정 만료 여부
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;  // 비밀번호 만료 여부
    }

    @Override
    public boolean isAccountNonLocked() {
        return this.user.getStatus() != UserStatus.BLOCKED;
    }

    @Override
    public boolean isEnabled() {
        return this.user.getStatus() == UserStatus.ACTIVE;
    }
}
