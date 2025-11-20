package com.ayno.aynobe.dto.user;

import com.ayno.aynobe.entity.enums.VisibilityType;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MyArtifactListItemResponseDTO {
    private Long artifactId;
    private String artifactTitle;
    private String thumbnailUrl;
    private Integer aiUsagePercent;
    private Long viewCount;
    private Long likeCount;
    private VisibilityType visibility;
    private String slug;
}
