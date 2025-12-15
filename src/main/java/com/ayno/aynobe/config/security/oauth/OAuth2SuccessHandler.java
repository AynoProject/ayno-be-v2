package com.ayno.aynobe.config.security.oauth;

import com.ayno.aynobe.config.security.CustomUserDetails;
import com.ayno.aynobe.config.security.service.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Locale;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2SuccessHandler implements AuthenticationSuccessHandler {

    private final JwtService jwtService;
    private final CookieFactory cookieFactory;

    @Value("${app.env:local}") private String appEnv; // local | dev | prod

    @Value("${app.frontend.redirect.local}") private String redirectLocal;
    @Value("${app.frontend.redirect.dev}")   private String redirectDev;
    @Value("${app.frontend.redirect.prod}")  private String redirectProd;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        CustomUserDetails principal = (CustomUserDetails) authentication.getPrincipal();

        String access  = jwtService.generateAccessToken(principal);
        String refresh = jwtService.generateRefreshToken(principal);

        response.addHeader(HttpHeaders.SET_COOKIE, cookieFactory.createUserAccess(access).toString());
        response.addHeader(HttpHeaders.SET_COOKIE, cookieFactory.createUserRefresh(refresh).toString());

        String env = appEnv.trim().toLowerCase(Locale.ROOT);
        String target = "dev".equals(env) ?
                redirectDev : "prod".equals(env) ?
                    redirectProd : redirectLocal;
        response.sendRedirect(target);
    }
}