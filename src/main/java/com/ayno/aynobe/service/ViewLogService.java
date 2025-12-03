package com.ayno.aynobe.service;

import com.ayno.aynobe.config.exception.CustomException;
import com.ayno.aynobe.config.security.CustomUserDetails;
import com.ayno.aynobe.dto.log.ViewLogStartRequestDTO;
import com.ayno.aynobe.dto.log.ViewLogEndRequestDTO;
import com.ayno.aynobe.dto.log.ViewLogResponseDTO;
import com.ayno.aynobe.entity.Artifact;
import com.ayno.aynobe.entity.User;
import com.ayno.aynobe.entity.log.ViewLog;
import com.ayno.aynobe.repository.ArtifactRepository;
import com.ayno.aynobe.repository.ViewLogRepository;
import io.micrometer.common.lang.Nullable;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ViewLogService {

    private final ViewLogRepository viewLogRepository;
    private final ArtifactRepository artifactRepository;

    @Transactional
    public ViewLogResponseDTO start(@Nullable CustomUserDetails principal, ViewLogStartRequestDTO req) {
        Long artifactId = req.getArtifactId();

        // 비로그인: 저장 안 함 → 조회수만 증가, viewId는 null
        if (principal == null) {
            artifactRepository.increaseViewCount(artifactId);
            return ViewLogResponseDTO.ofId(null);
        }

        Long userId = principal.getUser().getUserId();
        LocalDateTime enteredAt = LocalDateTime.now();
        LocalDate visitDate = enteredAt.toLocalDate();

        var existing = viewLogRepository.findByArtifact_ArtifactIdAndUser_UserIdAndVisitDate(
                artifactId, userId, visitDate);
        if (existing.isPresent()) {
            return ViewLogResponseDTO.from(existing.get());
        }

        try {
            Artifact artifactRef = artifactRepository.getReferenceById(artifactId);
            User userRef = principal.getUser();

            ViewLog created = ViewLog.builder()
                    .artifact(artifactRef)
                    .user(userRef)
                    .enteredAt(enteredAt)
                    .dwellSec(0L)
                    .visitDate(visitDate)
                    .build();

            created = viewLogRepository.save(created);
            artifactRepository.increaseViewCount(artifactId);
            return ViewLogResponseDTO.from(created);
        } catch (DataIntegrityViolationException dup) {
            var afterRace = viewLogRepository.findByArtifact_ArtifactIdAndUser_UserIdAndVisitDate(
                    artifactId, userId, visitDate
            ).orElseThrow(() -> dup);
            return ViewLogResponseDTO.from(afterRace);
        }
    }

    @Transactional
    public ViewLogResponseDTO end(@Nullable CustomUserDetails principal, ViewLogEndRequestDTO req) {
        Long artifactId = req.getArtifactId();

        // 비로그인: 저장 대상 아님 → null id 반환
        if (principal == null) return ViewLogResponseDTO.ofId(null);

        Long userId = principal.getUser().getUserId();
        LocalDateTime exitAt = LocalDateTime.now();
        LocalDate visitDate = exitAt.toLocalDate();

        var log = viewLogRepository.findByArtifact_ArtifactIdAndUser_UserIdAndVisitDate(
                artifactId, userId, visitDate
        ).orElseThrow(() ->
                CustomException.notFound("ViewLog not found for end(): start() was not called for today.")
        );

        log.endAt(exitAt); // dirty checking
        return ViewLogResponseDTO.from(log);
    }
}