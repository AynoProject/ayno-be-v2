package com.ayno.aynobe.controller.admin;

import com.ayno.aynobe.config.security.CustomAdminDetails;
import com.ayno.aynobe.config.security.oauth.CookieFactory;
import com.ayno.aynobe.dto.admin.AdminProfileResponseDTO;
import com.ayno.aynobe.dto.auth.LoginRequestDTO;
import com.ayno.aynobe.dto.auth.LoginResponseDTO;
import com.ayno.aynobe.dto.auth.LoginTokensDTO;
import com.ayno.aynobe.dto.common.Response;
import com.ayno.aynobe.service.admin.AdminAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "AdminAuth", description = "관리자 로그인 관련 API")
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AdminAuthService adminAuthService;
    private final CookieFactory cookieFactory;

    @Operation(
            summary = "관리자 로그인",
            description = "관리자 계정으로 JWT를 발급받습니다."
    )
    @PostMapping("/auth/login")
    public ResponseEntity<Response<LoginResponseDTO>> login(
            @RequestBody LoginRequestDTO request
    ) {
        LoginTokensDTO tokens = adminAuthService.login(request);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, cookieFactory.access(tokens.getAccessToken()).toString());
        headers.add(HttpHeaders.SET_COOKIE, cookieFactory.refresh(tokens.getRefreshToken()).toString());

        return ResponseEntity.ok()
                .headers(headers)
                .body(Response.success(new LoginResponseDTO("관리자 로그인 성공")));
    }

    @Operation(
            summary = "관리자 정보",
            description = "관리자 정보를 불러옵니다"
    )
    @GetMapping("/profile")
    public ResponseEntity<Response<AdminProfileResponseDTO>> getAdminProfile(
            @AuthenticationPrincipal CustomAdminDetails principal
    ) {
        return ResponseEntity.ok()
                .body(Response.success(adminAuthService.getAdminProfile(principal.getAdmin().getAdminId())));
    }
}
