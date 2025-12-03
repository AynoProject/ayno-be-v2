package com.ayno.aynobe.service;

import com.ayno.aynobe.config.exception.CustomException;
import com.ayno.aynobe.config.security.CustomUserDetails;
import com.ayno.aynobe.dto.report.ReportCreateRequestDTO;
import com.ayno.aynobe.entity.Report;
import com.ayno.aynobe.entity.User;
import com.ayno.aynobe.repository.ReportRepository;
import com.ayno.aynobe.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.apache.catalina.security.SecurityUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ReportService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;

    public void createReport(ReportCreateRequestDTO request, CustomUserDetails principal) {
        // 1. 신고자(로그인 유저) 조회
        User reporter = userRepository.findById(principal.getUser().getUserId())
                .orElseThrow(() -> CustomException.notFound("유저를 찾을 수 없습니다."));
        // 2. 중복 신고 체크 (이미 신고했으면 에러)
        if (reportRepository.existsByReporterAndTargetIdAndTargetType(reporter, request.getTargetId(), request.getTargetType())) {
            throw CustomException.duplicate("이미 신고한 대상입니다.");
        }

        // 3. (선택) TargetId가 실제 존재하는지 검증
        // MVP에서는 이 부분을 생략하거나,
        // 각 Repository(ArtifactRepository 등)를 주입받아 existsById 체크를 할 수 있음.
        // 여기선 일단 패스 (Admin에서 확인 가능하므로)

        // 4. 저장
        Report report = Report.builder()
                .reporter(reporter)
                .targetId(request.getTargetId())
                .targetType(request.getTargetType())
                .reason(request.getReason())
                .build();

        reportRepository.save(report);
    }
}
