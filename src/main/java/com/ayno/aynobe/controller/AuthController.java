package com.ayno.aynobe.controller;

import com.ayno.aynobe.config.security.oauth.CookieFactory;
import com.ayno.aynobe.dto.auth.*;
import com.ayno.aynobe.dto.common.Response;
import com.ayno.aynobe.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Auth", description = "로그인 및 가입 관련 API")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final CookieFactory cookieFactory;

    @Operation(
            summary = "로그인",
            description = "아이디와 비밀번호를 입력하여 JWT를 발급받습니다."
    )
    @PostMapping("/login")
    public ResponseEntity<Response<LoginResponseDTO>> login(
            @RequestBody LoginRequestDTO request
    ) {
        HttpHeaders headers = new HttpHeaders();
        LoginTokensDTO tokens = authService.login(request);

        headers.add(HttpHeaders.SET_COOKIE, cookieFactory.createUserAccess(tokens.getAccessToken()).toString());
        headers.add(HttpHeaders.SET_COOKIE, cookieFactory.createUserRefresh(tokens.getRefreshToken()).toString());

        return ResponseEntity
                .ok()
                .headers(headers)
                .body(Response.success(new LoginResponseDTO("로그인 성공")));
    }

    @Operation(
            summary = "회원가입",
            description = "아이디와 비밀번호를 입력하여 회원가입합니다"

    )
    @PostMapping("/signup")
    public ResponseEntity<Response<SignUpResponseDTO>> signUp(
            @RequestBody SignUpRequestDTO request
    ) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(Response.success(authService.signUp(request)));
    }

    @Operation(
            summary = "아이디 중복확인",
            description = "아이디 중복확인을 합니다"
    )
    @PostMapping("/check/username")
    public ResponseEntity<Response<DuplicationResponseDTO>> checkUsername(
            @RequestParam String username
    ){
        return ResponseEntity.ok()
                .body(Response.success(authService.checkUsername(username)));
    }
}
