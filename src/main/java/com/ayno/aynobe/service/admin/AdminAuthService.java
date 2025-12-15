package com.ayno.aynobe.service.admin;

import com.ayno.aynobe.config.exception.CustomException;
import com.ayno.aynobe.config.security.CustomAdminDetails;
import com.ayno.aynobe.config.security.service.JwtService;
import com.ayno.aynobe.dto.admin.AdminProfileResponseDTO;
import com.ayno.aynobe.dto.auth.*;
import com.ayno.aynobe.entity.Admin;
import com.ayno.aynobe.repository.AdminRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AdminAuthService {

    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final AdminRepository adminRepository;

    public AdminAuthService(@Qualifier("adminAuthManager") AuthenticationManager authenticationManager,
                            JwtService jwtService,  AdminRepository adminRepository) {
        this.authenticationManager = authenticationManager;
        this.jwtService = jwtService;
        this.adminRepository = adminRepository;
    }

    @Transactional
    public LoginTokensDTO login(LoginRequestDTO request) {

        var auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        CustomAdminDetails principal = (CustomAdminDetails) auth.getPrincipal();
        boolean isAdmin = principal.getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMIN".equals(a.getAuthority()));
        if (!isAdmin) {
            throw CustomException.forbidden("관리자 권한이 없습니다.");
        }

        String access  = jwtService.generateAccessToken(principal);
        String refresh = jwtService.generateRefreshToken(principal);

        return LoginTokensDTO.builder()
                .accessToken(access)
                .refreshToken(refresh)
                .build();
    }

    public AdminProfileResponseDTO getAdminProfile(Long adminId) {
        Admin admin = adminRepository.findById(adminId)
                .orElseThrow(() -> CustomException.notFound("관리자를 찾을 수 없습니다."));
        return AdminProfileResponseDTO.from(admin);
    }
}
