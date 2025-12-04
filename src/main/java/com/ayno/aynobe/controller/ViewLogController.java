package com.ayno.aynobe.controller;

import com.ayno.aynobe.config.security.CustomUserDetails;
import com.ayno.aynobe.dto.common.Response;
import com.ayno.aynobe.dto.log.ViewLogStartRequestDTO;
import com.ayno.aynobe.dto.log.ViewLogEndRequestDTO;
import com.ayno.aynobe.dto.log.ViewLogResponseDTO;
import com.ayno.aynobe.service.ViewLogService;
import io.micrometer.common.lang.Nullable;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "ViewLog", description = "아티팩트 조회 로그 API")
@RestController
@RequestMapping("/api/viewlogs")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('USER','ADMIN')")
public class ViewLogController {

    private final ViewLogService viewLogService;

    @Operation(summary = "조회 시작 로그", description = "로그인: 1일 1회 upsert(+viewCount). 비로그인: 저장 없이 viewCount만 증가")
    @PostMapping("/start")
    public ResponseEntity<Response<ViewLogResponseDTO>> start(
            @AuthenticationPrincipal @Nullable CustomUserDetails principal,
            @Valid @RequestBody ViewLogStartRequestDTO req
    ) {
        var result = viewLogService.start(principal, req);
        return ResponseEntity.ok(Response.success(result));
    }

    @Operation(summary = "조회 종료 로그", description = "로그인: dwellSec 갱신. 비로그인: no-op")
    @PostMapping("/end")
    public ResponseEntity<Response<ViewLogResponseDTO>> end(
            @AuthenticationPrincipal @Nullable CustomUserDetails principal,
            @Valid @RequestBody ViewLogEndRequestDTO req
    ) {
        var result = viewLogService.end(principal, req);
        return ResponseEntity.ok(Response.success(result));
    }
}
