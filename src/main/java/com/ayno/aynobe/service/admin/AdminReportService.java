package com.ayno.aynobe.service.admin;

import com.ayno.aynobe.config.exception.CustomException;
import com.ayno.aynobe.dto.admin.AdminReportResponseDTO;
import com.ayno.aynobe.dto.common.PageResponseDTO;
import com.ayno.aynobe.entity.Report;
import com.ayno.aynobe.entity.enums.ReportStatus;
import com.ayno.aynobe.entity.enums.ReportTargetType;
import com.ayno.aynobe.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.swing.undo.CannotUndoException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminReportService {

    private final ReportRepository reportRepository;

    public PageResponseDTO<AdminReportResponseDTO> getReports(
            ReportStatus status, ReportTargetType targetType, LocalDate from, LocalDate to, Pageable pageable
    ) {

        LocalDateTime startAt = (from != null) ? from.atStartOfDay() : null;
        LocalDateTime endAt = (to != null) ? to.atTime(LocalTime.MAX) : null;

        Page<Report> page = reportRepository.searchReports(status, targetType, startAt, endAt, pageable);

        List<AdminReportResponseDTO> content = page.getContent().stream()
                .map(AdminReportResponseDTO::from)
                .collect(Collectors.toList());

        return PageResponseDTO.<AdminReportResponseDTO>builder()
                .content(content)
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .hasNext(page.hasNext())
                .build();
    }

    @Transactional
    public void processReport(Long reportId, ReportStatus status, String memo) {
        Report report = reportRepository.findById(reportId)
                .orElseThrow(() -> CustomException.notFound("Report를 찾을 수 없습니다"));

        report.processReport(status, memo);

        // TODO: 여기서 만약 status가 RESOLVED(처리완료)라면,
        // targetType에 따라 해당 Artifact나 User를 BLOCKED 처리하는 로직을 추가할 수도 있음.
    }
}
