package com.ayno.aynobe.config.security.filter;

import com.ayno.aynobe.config.security.oauth.CookieFactory;
import com.ayno.aynobe.config.security.service.CustomAdminDetailsService;
import com.ayno.aynobe.config.security.service.CustomUserDetailsService;
import com.ayno.aynobe.config.security.service.JwtService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AccountStatusException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final CustomUserDetailsService userDetailsService;
    private final CustomAdminDetailsService adminDetailsService;
    private final CookieFactory cookieFactory;
    private final JsonAccessDeniedHandler accessDeniedHandler;

    private static final List<String> WHITELIST_PREFIXES = List.of(
            "/api/auth", "/api/admin/auth", "/swagger-ui", "/swagger-ui.html", "/v3/api-docs", "/h2-console"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String uri = request.getRequestURI();

        // 1. 화이트리스트 및 이미 인증된 경우 패스
        if (isWhitelisted(uri) || isAlreadyAuthenticated()) {
            chain.doFilter(request, response);
            return;
        }

        try {
            // 2. Access Token으로 인증 시도
            if (tryAccessTokenAuthentication(request, uri)) {
                chain.doFilter(request, response);
                return;
            }

            // 3. Access 실패 시, Refresh Token으로 재발급 및 인증 시도
            if (tryRefreshTokenAuthentication(request, response, uri)) {
                chain.doFilter(request, response);
                return;
            }
        } catch (AccountStatusException e) {
            // 계정 정지/잠금 등 명확한 거부 사유 발생 시 핸들링
            accessDeniedHandler.handle(request, response, new AccessDeniedException(e.getMessage()));
            return;
        }

        // 4. 인증 실패했거나 토큰이 없는 경우 -> 익명 사용자로 다음 필터 진행
        chain.doFilter(request, response);
    }

    // --- [Core Logic 1] Access Token 처리 ---
    private boolean tryAccessTokenAuthentication(HttpServletRequest request, String uri) {
        String token = resolveToken(request, uri, CookieFactory.ADMIN_ACCESS_COOKIE, CookieFactory.USER_ACCESS_COOKIE);
        if (token == null) return false;

        try {
            var tokenInfo = jwtService.payload(token);
            UserDetails principal = loadByRoles(tokenInfo.subject(), tokenInfo.roles());

            if (jwtService.isTokenValid(tokenInfo, principal)) {
                setAuthentication(request, principal);
                return true;
            }
        } catch (RuntimeException ignored) {
            // 토큰 만료, 서명 불일치 등 -> Refresh 시도를 위해 false 반환
        }
        return false;
    }

    // --- [Core Logic 2] Refresh Token 처리 (재발급) ---
    private boolean tryRefreshTokenAuthentication(HttpServletRequest request, HttpServletResponse response, String uri) {
        String token = resolveToken(request, uri, CookieFactory.ADMIN_REFRESH_COOKIE, CookieFactory.USER_REFRESH_COOKIE);
        if (token == null) return false;

        try {
            var tokenInfo = jwtService.payload(token);
            if (jwtService.isRefreshTokenValid(tokenInfo)) {
                UserDetails principal = loadByRoles(tokenInfo.subject(), tokenInfo.roles());
                String newAccessToken = jwtService.generateAccessToken(principal);

                // 쿠키 재발급 (Role 기반)
                reissueCookie(response, newAccessToken, tokenInfo.roles());

                // 컨텍스트 설정
                setAuthentication(request, principal);
                return true;
            }
        } catch (RuntimeException ignored) {
            // Refresh Token도 유효하지 않음 -> 로그인 필요
        }
        return false;
    }

    // --- [Helpers] ---

    private void reissueCookie(HttpServletResponse response, String accessToken, List<String> roles) {
        boolean isAdmin = roles != null && roles.contains("ROLE_ADMIN");
        ResponseCookie cookie = isAdmin
                ? cookieFactory.createAdminAccess(accessToken)
                : cookieFactory.createUserAccess(accessToken);
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    // 통합된 토큰 조회 로직 (중복 제거)
    private String resolveToken(HttpServletRequest request, String uri, String adminCookieName, String userCookieName) {
        if (uri.startsWith("/api/admin")) {
            return getCookieValue(request, adminCookieName);
        }
        // 공용 경로: 유저 -> 관리자 순 확인
        String token = getCookieValue(request, userCookieName);
        if (token != null) return token;
        return getCookieValue(request, adminCookieName);
    }

    private UserDetails loadByRoles(String username, List<String> roles) {
        if (roles != null && roles.contains("ROLE_ADMIN")) {
            return adminDetailsService.loadUserByUsername(username);
        }
        return userDetailsService.loadUserByUsername(username);
    }

    private void setAuthentication(HttpServletRequest request, UserDetails user) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private boolean isAlreadyAuthenticated() {
        return SecurityContextHolder.getContext().getAuthentication() != null;
    }

    private boolean isWhitelisted(String uri) {
        return WHITELIST_PREFIXES.stream().anyMatch(uri::startsWith);
    }

    private String getCookieValue(HttpServletRequest request, String name) {
        if (request.getCookies() == null) return null;
        for (Cookie c : request.getCookies()) {
            if (name.equals(c.getName())) return c.getValue();
        }
        return null;
    }
}