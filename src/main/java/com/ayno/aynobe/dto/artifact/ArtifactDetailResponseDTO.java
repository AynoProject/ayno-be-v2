package com.ayno.aynobe.dto.artifact;

import com.ayno.aynobe.entity.Artifact;
import com.ayno.aynobe.entity.enums.FlowType;
import com.ayno.aynobe.entity.enums.VisibilityType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(name = "ArtifactDetailResponse")
public class ArtifactDetailResponseDTO {
    private Long artifactId;
    private String artifactTitle;
    private String thumbnailUrl;
    private Integer aiUsagePercent;
    private Long viewCount;
    private Long likeCount;
    private VisibilityType visibility;
    private String slug;
    private FlowType category;
    private Long userId;
    private String nickname;
    private String profileImageUrl;
    private Long workflowId;          // nullable
    private List<ArtifactMediaResponseDTO> medias;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static ArtifactDetailResponseDTO from(Artifact a) {
        return ArtifactDetailResponseDTO.builder()
                .artifactId(a.getArtifactId())
                .artifactTitle(a.getArtifactTitle())
                .thumbnailUrl(a.getThumbnailUrl())
                .aiUsagePercent(a.getAiUsagePercent())
                .viewCount(a.getViewCount())
                .likeCount(a.getLikeCount())
                .visibility(a.getVisibility())
                .slug(a.getSlug())
                .category(a.getCategory())
                .userId(a.getUser().getUserId())
                .nickname(a.getUser().getNickname())
                .profileImageUrl(a.getUser().getProfileImageUrl())
                .workflowId(a.getWorkflow() != null ? a.getWorkflow().getWorkflowId() : null)
                .medias(a.getMedias().stream().map(ArtifactMediaResponseDTO::from).toList())
                .createdAt(a.getCreatedAt())
                .updatedAt(a.getUpdatedAt())
                .build();
    }
}
