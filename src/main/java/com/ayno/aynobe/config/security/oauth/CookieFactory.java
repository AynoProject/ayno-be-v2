package com.ayno.aynobe.config.security.oauth;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@RequiredArgsConstructor
public class CookieFactory {

    @Value("${app.env:local}") private String appEnv; // local | dev | prod

    public static final String USER_ACCESS_COOKIE = "user_accessToken";
    public static final String USER_REFRESH_COOKIE = "user_refreshToken";
    public static final String ADMIN_ACCESS_COOKIE = "admin_accessToken";
    public static final String ADMIN_REFRESH_COOKIE = "admin_refreshToken";

    // 1. 유저용 쿠키 생성
    public ResponseCookie createUserAccess(String token) {
        return build(USER_ACCESS_COOKIE, token, Duration.ofMinutes(15));
    }
    public ResponseCookie createUserRefresh(String token) {
        return build(USER_REFRESH_COOKIE, token, Duration.ofDays(7));
    }

    // 2. 관리자용 쿠키 생성
    public ResponseCookie createAdminAccess(String token) {
        return build(ADMIN_ACCESS_COOKIE, token, Duration.ofMinutes(15));
    }
    public ResponseCookie createAdminRefresh(String token) {
        return build(ADMIN_REFRESH_COOKIE, token, Duration.ofDays(7));
    }

    // 3. 로그아웃용 (쿠키 삭제)
    public ResponseCookie clearUserAccess() {
        return build(USER_ACCESS_COOKIE, "", Duration.ZERO);
    }
    public ResponseCookie clearUserRefresh() {
        return build(USER_REFRESH_COOKIE, "", Duration.ZERO);
    }
    public ResponseCookie clearAdminAccess() {
        return build(ADMIN_ACCESS_COOKIE, "", Duration.ZERO);
    }
    public ResponseCookie clearAdminRefresh() {
        return build(ADMIN_REFRESH_COOKIE, "", Duration.ZERO);
    }

    private ResponseCookie build(String name, String value, Duration maxAge) {
        boolean secure = !"local".equalsIgnoreCase(appEnv); // dev/prod: https
        String sameSite = secure ? "None" : "Lax";           // None은 Secure 필수
        return ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path("/")
                .maxAge(maxAge)
                .build();
    }
}