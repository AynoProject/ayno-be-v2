package com.ayno.aynobe.dto.admin;

import com.ayno.aynobe.entity.Report;
import com.ayno.aynobe.entity.enums.ReportStatus;
import com.ayno.aynobe.entity.enums.ReportTargetType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminReportResponseDTO {
    // 신고 기본 정보
    private Long reportId;
    private String reason;
    private ReportStatus status;       // PENDING, RESOLVED...
    private LocalDateTime createdAt;   // 신고 일시
    private String adminMemo;          // 관리자 메모 (처리 결과 등)

    // 신고자 정보 (N+1 방지 위해 Fetch Join된 데이터 사용)
    private Long reporterId;
    private String reporterEmail;      // username
    private String reporterNickname;

    // 신고 대상 정보
    private Long targetId;
    private ReportTargetType targetType;

    // Entity -> DTO 변환 메서드
    public static AdminReportResponseDTO from(Report report) {
        return AdminReportResponseDTO.builder()
                .reportId(report.getReportId())
                .reason(report.getReason())
                .status(report.getStatus())
                .createdAt(report.getCreatedAt())
                .adminMemo(report.getAdminMemo())
                // 신고자 정보 평탄화 (Flatten)
                .reporterId(report.getReporter().getUserId())
                .reporterEmail(report.getReporter().getUsername())
                .reporterNickname(report.getReporter().getNickname())
                // 대상 정보
                .targetId(report.getTargetId())
                .targetType(report.getTargetType())
                .build();
    }
}
