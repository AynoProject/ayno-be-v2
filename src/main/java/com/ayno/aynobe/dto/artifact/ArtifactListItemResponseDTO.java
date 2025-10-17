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
    private Integer aiUsagePercent;
    private Long viewCount;
    private Long likeCount;
    private VisibilityType visibility;
    private String slug;
    @Schema(description = "연결된 워크플로우 ID (없으면 null)")
    private Long workflowId;

    public static ArtifactListItemResponseDTO from(Artifact artifact) {
        return ArtifactListItemResponseDTO.builder()
                .artifactId(artifact.getArtifactId())
                .artifactTitle(artifact.getArtifactTitle())
                .aiUsagePercent(artifact.getAiUsagePercent())
                .viewCount(artifact.getViewCount())
                .likeCount(artifact.getLikeCount())
                .visibility(artifact.getVisibility())
                .slug(artifact.getSlug())
                .workflowId(artifact.getWorkflow().getWorkflowId())
                .build();
    }
}
