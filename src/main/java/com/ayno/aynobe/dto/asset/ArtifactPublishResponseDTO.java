package com.ayno.aynobe.dto.asset;

import com.ayno.aynobe.entity.enums.VisibilityType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(name = "ArtifactPublishResponse")
public class ArtifactPublishResponseDTO {

    @Schema(description = "발행된 결과물 ID", example = "123")
    private Long artifactId;

    @Schema(description = "발행 후 공개상태", example = "PUBLIC")
    private VisibilityType visibility;

    @Schema(description = "발행 처리된 미디어 개수", example = "3")
    private Integer publishedMediaCount;
}
