package com.ayno.aynobe.service.admin;

import com.ayno.aynobe.config.exception.CustomException;
import com.ayno.aynobe.dto.admin.AdminArtifactResponseDTO;
import com.ayno.aynobe.dto.common.PageResponseDTO;
import com.ayno.aynobe.entity.Artifact;
import com.ayno.aynobe.entity.ArtifactMedia;
import com.ayno.aynobe.entity.Workflow;
import com.ayno.aynobe.entity.enums.ReportTargetType;
import com.ayno.aynobe.entity.enums.TargetType;
import com.ayno.aynobe.entity.enums.VisibilityType;
import com.ayno.aynobe.repository.ArtifactRepository;
import com.ayno.aynobe.repository.ReactionRepository;
import com.ayno.aynobe.repository.ReportRepository;
import com.ayno.aynobe.repository.StepSectionRepository;
import com.ayno.aynobe.service.s3.S3Service;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminArtifactService {
    private final ArtifactRepository artifactRepository;
    private final ReportRepository reportRepository;
    private final ReactionRepository reactionRepository;
    private final StepSectionRepository stepSectionRepository;
    private final S3Service s3Service;

    @Transactional(readOnly = true)
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

    @Transactional
    public void deleteArtifact(Long artifactId) {
        Artifact artifact = artifactRepository.findById(artifactId)
                .orElseThrow(() -> CustomException.notFound("Artifact not found"));

        // S3 물리적 파일 삭제
        Workflow workflow = artifact.getWorkflow();
        if (workflow != null) {

            Long workflowId = workflow.getWorkflowId();

            // 워크플로우 ID로 모든 섹션의 baseKey를 한번에 조회 (N+1 방지)
            List<String> sectionBaseKeys = stepSectionRepository.findAllBaseKeysByWorkflowId(workflowId);

            for (String baseKey : sectionBaseKeys) {
                if (baseKey != null && !baseKey.isBlank()) {
                    s3Service.deleteS3MediaSet(baseKey);
                }
            }
        }
        // 아티팩트 본문 미디어 삭제
        for (ArtifactMedia media : artifact.getMedias()) {
            s3Service.deleteS3MediaSet(media.getBaseKey());
        }


        reportRepository.deleteByTargetIdAndTargetType(artifactId, ReportTargetType.ARTIFACT);

        reactionRepository.deleteByTargetIdAndTargetType(artifactId, TargetType.ARTIFACT);

        artifactRepository.delete(artifact);
    }
}
