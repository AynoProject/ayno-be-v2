package com.ayno.aynobe.service.admin;

import com.ayno.aynobe.config.exception.CustomException;
import com.ayno.aynobe.dto.admin.AdminArtifactResponseDTO;
import com.ayno.aynobe.dto.common.PageResponseDTO;
import com.ayno.aynobe.entity.Artifact;
import com.ayno.aynobe.entity.enums.ReportTargetType;
import com.ayno.aynobe.entity.enums.TargetType;
import com.ayno.aynobe.entity.enums.VisibilityType;
import com.ayno.aynobe.repository.ArtifactRepository;
import com.ayno.aynobe.repository.ReactionRepository;
import com.ayno.aynobe.repository.ReportRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminArtifactService {
    private final ArtifactRepository artifactRepository;
    private final ReportRepository reportRepository;
    private final ReactionRepository reactionRepository;

    public PageResponseDTO<AdminArtifactResponseDTO> getArtifacts(
            VisibilityType status,
            String keyword,
            Pageable pageable
    ) {
        // 숫자가 들어오면 ID로 인식
        Long artifactId = null;
        if (keyword != null && keyword.matches("^[0-9]+$")) {
            artifactId = Long.parseLong(keyword);
        }

        Page<Artifact> artifactPage = artifactRepository.searchArtifactsForAdmin(status, artifactId, keyword, pageable);

        List<AdminArtifactResponseDTO> content = artifactPage.getContent().stream()
                .map(AdminArtifactResponseDTO::from)
                .toList();

        return PageResponseDTO.<AdminArtifactResponseDTO>builder()
                .content(content)
                .page(artifactPage.getNumber())
                .size(artifactPage.getSize())
                .totalElements(artifactPage.getTotalElements())
                .totalPages(artifactPage.getTotalPages())
                .hasNext(artifactPage.hasNext())
                .build();
    }

    public void deleteArtifact(Long artifactId) {
        Artifact artifact = artifactRepository.findById(artifactId)
                .orElseThrow(() -> CustomException.notFound("Artifact not found"));

        // 신고 내역 삭제
        reportRepository.deleteByTargetIdAndTargetType(artifactId, ReportTargetType.ARTIFACT);
        // 좋아요 삭제
        reactionRepository.deleteByTargetIdAndTargetType(artifactId, TargetType.ARTIFACT);

        artifactRepository.delete(artifact);
    }
}
