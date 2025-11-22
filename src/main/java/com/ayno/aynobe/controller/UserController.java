package com.ayno.aynobe.controller;

import com.ayno.aynobe.config.security.CustomUserDetails;
import com.ayno.aynobe.dto.common.PageResponseDTO;
import com.ayno.aynobe.dto.common.Response;
import com.ayno.aynobe.dto.user.*;
import com.ayno.aynobe.entity.enums.VisibilityType;
import com.ayno.aynobe.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "User", description = "사용자 관련 API")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@PreAuthorize("hasRole('USER')")
public class UserController {

    private final UserService userService;

    @Operation(
            summary = "내 온보딩 정보 조회"
    )
    @GetMapping("/me/onboarding")
    public ResponseEntity<Response<OnboardingResponseDTO>> getMyOnboarding(
            @AuthenticationPrincipal CustomUserDetails principal
    ) {
        return ResponseEntity.ok()
                .body(Response.success(userService.getMyOnboarding(principal.getUser().getUserId())));
    }

    @Operation(
            summary = "내 온보딩 정보 저장(Upsert/부분수정)",
            description = """
            - null 필드는 변경하지 않습니다.
            - interests: null=미변경, []=전체 해제, [ids]=교체(최대 3)
            - 멱등한 PUT. JPA 더티체킹으로 반영됩니다.
            """
    )
    @PutMapping("/me/onboarding")
    public ResponseEntity<Response<OnboardingResponseDTO>> upsertOnboarding(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestBody OnboardingUpsertRequestDTO request
    ) {
        return ResponseEntity.ok()
                .body(Response.success(userService.upsertOnboarding(principal.getUser().getUserId(), request)));
    }

    @Operation(
            summary = "내 프로필 정보 가져오기"
    )
    @GetMapping("/me/profile")
    public ResponseEntity<Response<ProfileResponseDTO>> getMyProfile(
            @AuthenticationPrincipal CustomUserDetails principal
    ){
        return ResponseEntity.ok()
                .body(Response.success(userService.getMyProfile(principal.getUser().getUserId())));
    }

    @Operation(summary = "내 프로필 수정", description = "닉네임, 직무, 관심사, 기타 정보를 수정합니다. (이미지 제외)")
    @PutMapping("/me/profile")
    public ResponseEntity<Response<ProfileResponseDTO>> updateProfile(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestBody ProfileUpdateRequestDTO request
    ) {
        return ResponseEntity.ok()
                .body(Response.success(
                        userService.updateProfile(principal.getUser().getUserId(), request)
                ));
    }

    @Operation(
            summary = "내 결과물 목록 가져오기"
    )
    @GetMapping("/me/artifact")
    public ResponseEntity<Response<PageResponseDTO<MyArtifactListItemResponseDTO>>> getMyArtifact(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestParam(required = false) VisibilityType visibility,
            @ParameterObject Pageable pageable
    ){
        return ResponseEntity.ok()
                .body(Response.success(userService.getMyArtifact(principal.getUser().getUserId(), visibility ,pageable)));
    }

    @Operation(summary = "내가 좋아요 누른 작품 목록 조회")
    @GetMapping("/me/likes")
    public ResponseEntity<Response<PageResponseDTO<MyArtifactListItemResponseDTO>>> getLikedArtifacts(
            @AuthenticationPrincipal CustomUserDetails principal,
            @ParameterObject Pageable pageable
    ) {
        return ResponseEntity.ok()
                .body(Response.success(userService.getLikedArtifacts(principal.getUser().getUserId(), pageable)));
    }


}
