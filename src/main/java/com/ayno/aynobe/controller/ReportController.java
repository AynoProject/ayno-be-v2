package com.ayno.aynobe.controller;

import com.ayno.aynobe.config.security.CustomUserDetails;
import com.ayno.aynobe.dto.common.Response;
import com.ayno.aynobe.dto.report.ReportCreateRequestDTO;
import com.ayno.aynobe.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Report", description = "신고 기능 (사용자용)")
@RestController
@RequestMapping("/api/reports")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @Operation(summary = "신고하기", description = "유저 또는 결과물을 신고합니다.")
    @PostMapping
    public ResponseEntity<?> createReport(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestBody @Valid ReportCreateRequestDTO reportCreateRequestDTO
    ) {
        reportService.createReport(reportCreateRequestDTO, principal);
        return ResponseEntity.ok(Response.success("신고가 접수되었습니다."));
    }
}
