package com.ayno.aynobe.entity;

import com.ayno.aynobe.entity.enums.ReportStatus;
import com.ayno.aynobe.entity.enums.ReportTargetType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "reports")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Report extends BaseTimeEntity{
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long reportId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporterId", nullable = false)
    private User reporter; // 신고자

    @Column(nullable = false)
    private Long targetId; // 신고 대상

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportTargetType targetType;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ReportStatus status; // reportStatus -> status로 통일 권장

    @Column(columnDefinition = "TEXT")
    private String adminMemo; // 처리 결과 메모

    public Report(User reporter, Long targetId, ReportTargetType targetType, String reason) {
        this.reporter = reporter;
        this.targetId = targetId;
        this.targetType = targetType;
        this.reason = reason;
        this.status = ReportStatus.PENDING;
    }

    public void processReport(ReportStatus status, String adminMemo) {
        this.status = status;
        this.adminMemo = adminMemo;
    }
}
