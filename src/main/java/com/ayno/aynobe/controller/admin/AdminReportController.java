package com.ayno.aynobe.controller.admin;

import com.ayno.aynobe.dto.admin.AdminReportResponseDTO;
import com.ayno.aynobe.dto.admin.ReportProcessRequestDTO;
import com.ayno.aynobe.dto.common.PageResponseDTO;
import com.ayno.aynobe.dto.common.Response;
import com.ayno.aynobe.entity.enums.ReportStatus;
import com.ayno.aynobe.entity.enums.ReportTargetType;
import com.ayno.aynobe.service.admin.AdminReportService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Tag(name = "AdminReport", description = "관리자 신고 내역 관리")
@RestController
@RequestMapping("/api/admin/reports")
@RequiredArgsConstructor
public class AdminReportController {

    private final AdminReportService adminReportService;

    @GetMapping
    public ResponseEntity<Response<PageResponseDTO<AdminReportResponseDTO>>> getReports(
            @RequestParam(required = false) ReportStatus status,
            @RequestParam(required = false) ReportTargetType targetType,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @ParameterObject Pageable pageable
    ) {
        return ResponseEntity.ok(Response.success(
                adminReportService.getReports(status, targetType, from, to, pageable)
        ));
    }

    @PatchMapping("/{reportId}")
    public ResponseEntity<?> processReport(
            @PathVariable Long reportId,
            @RequestBody ReportProcessRequestDTO body // { "status": "RESOLVED", "memo": "욕설 확인" }
    ) {
        adminReportService.processReport(reportId, body.getStatus(), body.getMemo());
        return ResponseEntity.ok(Response.success("신고가 처리되었습니다."));
    }

}
