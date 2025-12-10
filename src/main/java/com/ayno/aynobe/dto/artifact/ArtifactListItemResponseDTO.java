package com.ayno.aynobe.dto.artifact;

import com.ayno.aynobe.entity.Artifact;
import com.ayno.aynobe.entity.enums.VisibilityType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(name = "ArtifactListItemResponse")
public class ArtifactListItemResponseDTO {
    private Long artifactId;
    private String artifactTitle;
    private String thumbnailUrl;
    private Long userId;
    private String nickname;
    private String profileImageUrl;
    private Integer aiUsagePercent;
    private Long viewCount;
    private Long likeCount;
    private VisibilityType visibility;
    private String slug;
    @Schema(description = "연결된 워크플로우 ID (없으면 null)")
    private Long workflowId;

    public static ArtifactListItemResponseDTO from(Artifact artifact) {
        Long workflowId = (artifact.getWorkflow() != null)
                ? artifact.getWorkflow().getWorkflowId()
                : null;

        return ArtifactListItemResponseDTO.builder()
                .artifactId(artifact.getArtifactId())
                .artifactTitle(artifact.getArtifactTitle())
                .thumbnailUrl(artifact.getThumbnailUrl())
                .userId(artifact.getUser().getUserId())
                .nickname(artifact.getUser().getNickname())
                .profileImageUrl(artifact.getUser().getProfileImageUrl())
                .aiUsagePercent(artifact.getAiUsagePercent())
                .viewCount(artifact.getViewCount())
                .likeCount(artifact.getLikeCount())
                .visibility(artifact.getVisibility())
                .slug(artifact.getSlug())
                .workflowId(workflowId)
                .build();
    }
}
