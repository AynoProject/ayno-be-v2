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

    // 화이트리스트 (인증 제외)
    private static final List<String> WHITELIST_PREFIXES = List.of(
            "/api/auth",
            "/api/admin/auth",
            "/swagger-ui",
            "/swagger-ui.html",
            "/v3/api-docs",
            "/h2-console"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain)
            throws ServletException, IOException {

        final String uri = request.getRequestURI();
        if (isWhitelisted(uri)) {
            chain.doFilter(request, response);
            return;
        }

        // 이미 인증돼 있으면 패스
        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            chain.doFilter(request, response);
            return;
        }

        boolean isAdminEndpoint = uri.startsWith("/api/admin");

        String accessCookieName = isAdminEndpoint ? CookieFactory.ADMIN_ACCESS_COOKIE : CookieFactory.USER_ACCESS_COOKIE;
        String refreshCookieName = isAdminEndpoint ? CookieFactory.ADMIN_REFRESH_COOKIE : CookieFactory.USER_REFRESH_COOKIE;

        // accessToken 우선 시도
        String accessToken = getCookieValue(request, accessCookieName);
        if (accessToken != null) {
            try {
                processTokenAuthentication(accessToken);
                chain.doFilter(request, response);
                return;
            } catch (AccountStatusException e) {
                accessDeniedHandler.handle(request, response, new AccessDeniedException(e.getMessage()));
                return;
            } catch (RuntimeException ignored) {}
        }

        // access 없거나 만료 → refresh 로 재발급 시도
        String refreshToken = getCookieValue(request, refreshCookieName);
        if (refreshToken != null) {
            try {
                var tokenInfor = jwtService.payload(refreshToken);
                if (jwtService.isRefreshTokenValid(tokenInfor)) {
                    UserDetails principal = loadByRoles(tokenInfor.subject(), tokenInfor.roles());
                    String newAccess = jwtService.generateAccessToken(principal);

                    ResponseCookie newCookie = isAdminEndpoint
                            ? cookieFactory.createAdminAccess(newAccess)
                            : cookieFactory.createUserAccess(newAccess);

                    response.addHeader(HttpHeaders.SET_COOKIE, newCookie.toString());
                    setAuthentication(request, principal);
                }
            } catch (AccountStatusException e) {
                accessDeniedHandler.handle(request, response, new AccessDeniedException(e.getMessage()));
                return;
            }
            catch (RuntimeException ignored) {}
        }
        chain.doFilter(request, response);
    }

    // 헬퍼
    private void processTokenAuthentication(String token) {
        var tokenInfo = jwtService.payload(token);
        UserDetails principal = loadByRoles(tokenInfo.subject(), tokenInfo.roles());
        if (jwtService.isTokenValid(tokenInfo, principal)) {
            setAuthentication(null, principal); // request는 필요 없어서 null 처리하거나 수정
        }
    }

    private boolean isWhitelisted(String uri) {
        for (String prefix : WHITELIST_PREFIXES) {
            if (uri.startsWith(prefix)) return true;
        }
        return false;
    }

    private String getCookieValue(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) return null;
        for (Cookie c : cookies) {
            if (name.equals(c.getName())) return c.getValue();
        }
        return null;
    }

    private void setAuthentication(HttpServletRequest request, UserDetails user) {
        var auth = new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private UserDetails loadByRoles(String username, List<String> roles) {
        // roles 기준으로 어떤 DetailsService를 쓸지 결정
        if (roles != null && roles.contains("ROLE_ADMIN")) {
            return adminDetailsService.loadUserByUsername(username);
        }
        return userDetailsService.loadUserByUsername(username);
    }
}