package com.ayno.aynobe.dto.admin;

import com.ayno.aynobe.entity.Artifact;
import com.ayno.aynobe.entity.enums.VisibilityType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "관리자용 프로젝트 목록 응답 (썸네일 제외, 관리 정보 포함)")
public class AdminArtifactResponseDTO {
    private Long artifactId;
    private String title;
    private Long workflowId; // 워크플로우 연결 여부 확인용
    private Long userId;
    private String nickname;
    private String username;
    private VisibilityType visibility; // PUBLIC, PRIVATE
    private Long viewCount;            // 영향력 파악용
    private LocalDateTime createdAt;

    // Entity -> DTO 변환 메서드
    public static AdminArtifactResponseDTO from(Artifact artifact) {
        return AdminArtifactResponseDTO.builder()
                .artifactId(artifact.getArtifactId())
                .title(artifact.getArtifactTitle())
                .workflowId(artifact.getWorkflow() != null ? artifact.getWorkflow().getWorkflowId() : null)
                .userId(artifact.getUser().getUserId())
                .nickname(artifact.getUser().getNickname())
                .username(artifact.getUser().getUsername())
                .visibility(artifact.getVisibility())
                .viewCount(artifact.getViewCount())
                .createdAt(artifact.getCreatedAt())
                .build();
    }
}
