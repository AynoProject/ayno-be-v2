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

    public ResponseCookie access(String token) {
        return build("accessToken", token, Duration.ofMinutes(15));
    }

    public ResponseCookie refresh(String token) {
        return build("refreshToken", token, Duration.ofDays(7));
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