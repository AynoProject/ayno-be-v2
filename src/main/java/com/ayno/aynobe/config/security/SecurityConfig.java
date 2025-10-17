package com.ayno.aynobe.config.security;

import com.ayno.aynobe.config.security.filter.JsonAccessDeniedHandler;
import com.ayno.aynobe.config.security.filter.JsonAuthenticationEntryPoint;
import com.ayno.aynobe.config.security.filter.JwtAuthenticationFilter;
import com.ayno.aynobe.config.security.oauth.OAuth2SuccessHandler;
import com.ayno.aynobe.config.security.service.CustomAdminDetailsService;
import com.ayno.aynobe.config.security.service.CustomOAuth2UserService;
import com.ayno.aynobe.config.security.service.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;

    private final JsonAuthenticationEntryPoint jsonAuthenticationEntryPoint;
    private final JsonAccessDeniedHandler jsonAccessDeniedHandler;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf((auth) -> auth.disable())
                .formLogin((auth) -> auth.disable())
                .cors(c -> c.configurationSource(corsConfigurationSource()))
                .httpBasic((auth) -> auth.disable())
                .sessionManagement((session) -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .headers(headers -> headers
                        .frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin)
                )
                .exceptionHandling(h -> h
                        .authenticationEntryPoint(jsonAuthenticationEntryPoint)
                        .accessDeniedHandler(jsonAccessDeniedHandler)
                )
                //경로별 인가 작업
                .authorizeHttpRequests((auth) -> auth
                        .requestMatchers("/swagger-ui.html",
                                "/swagger-ui/**",
                                "/api-docs/**",
                                "/v3/api-docs/**",
                                "/h2-console/**").permitAll()
                        .requestMatchers("/api/auth/**", "/api/admin/auth/**", "/api/viewlogs/**").permitAll()
                        .requestMatchers("/oauth2/**","/login/oauth2/**").permitAll()
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .requestMatchers("/api/interests/**", "/api/jobs/**").hasAnyRole("ADMIN","USER")
                        .requestMatchers(HttpMethod.GET, "/api/workflows/**", "/api/reaction/**", "/api/artifacts/**").permitAll()
                        .requestMatchers("/api/users/**", "/api/workflows/**", "/api/reaction/**", "/api/artifacts/**").hasRole("USER")
                        .anyRequest().authenticated())
                .oauth2Login(oauth2 -> oauth2
                .userInfoEndpoint(userInfo -> userInfo
                        .userService(customOAuth2UserService))
                        .successHandler(oAuth2SuccessHandler))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
                "http://localhost:5173",
                "https://www.ayno.co.kr",
                "https://api.ayno.co.kr"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);

        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }


    @Bean
    public DaoAuthenticationProvider userAuthenticationProvider(
            PasswordEncoder encoder,
            CustomUserDetailsService userDetailsService
    ) {
        DaoAuthenticationProvider p = new DaoAuthenticationProvider();
        p.setPasswordEncoder(encoder);
        p.setUserDetailsService(userDetailsService);
        return p;
    }

    @Bean
    public DaoAuthenticationProvider adminAuthenticationProvider(
            PasswordEncoder encoder,
            CustomAdminDetailsService adminDetailsService
    ) {
        DaoAuthenticationProvider p = new DaoAuthenticationProvider();
        p.setPasswordEncoder(encoder);
        p.setUserDetailsService(adminDetailsService);
        return p;
    }

    // 유저 전용 AuthenticationManager
    @Bean(name = "userAuthManager")
    @Primary
    public AuthenticationManager userAuthenticationManager(
            DaoAuthenticationProvider userAuthenticationProvider
    ) {
        return new ProviderManager(userAuthenticationProvider);
    }

    // 관리자 전용 AuthenticationManager
    @Bean(name = "adminAuthManager")
    public AuthenticationManager adminAuthenticationManager(
            DaoAuthenticationProvider adminAuthenticationProvider
    ) {
        return new ProviderManager(adminAuthenticationProvider);
    }

}