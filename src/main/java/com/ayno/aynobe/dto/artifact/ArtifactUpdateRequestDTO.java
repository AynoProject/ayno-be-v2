package com.ayno.aynobe.dto.artifact;

import com.ayno.aynobe.entity.enums.FlowType;
import com.ayno.aynobe.entity.enums.VisibilityType;
import com.ayno.aynobe.entity.enums.MediaType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(name = "ArtifactUpdateRequest")
public class ArtifactUpdateRequestDTO {

    @NotNull
    private FlowType category;

    @NotBlank @Size(max = 100)
    private String artifactTitle;

    @NotNull
    private VisibilityType visibility;

    @NotNull @Min(0) @Max(100)
    private Integer aiUsagePercent;

    @Builder.Default
    private Boolean isPremium = false;

    @NotBlank @Size(max = 256)
    private String slug;

    /** 연결 워크플로우(선택, null 가능) */
    private Long workflowId;

    /** 미디어 전체 교체(MVP: 전체 replace) */
    @Valid
    @Builder.Default
    private List<MediaDTO> medias = List.of();

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(name = "ArtifactUpdateMedia")
    public static class MediaDTO {
        @NotNull
        private MediaType mediaType;

        @NotBlank
        @Size(max = 512)
        @Schema(
                description = "S3 상대 경로(baseKey). original.ext까지 포함",
                example = "artifact/123/media/5e6b7bfa-cc41-47f3-86b5-f542d9cfb49a/original.ext"
        )
        private String baseKey;

        /** 정렬 순서 (NOT NULL) */
        @Schema(description = "정렬 순서(오름차순)", example = "0")
        private int sortOrder;

        /** 커버 여부 (NOT NULL) */
        @Schema(description = "커버(대표) 이미지 여부", example = "false")
        private boolean isCover;

        /** 캡션만 NULL 허용 */
        @Size(max = 256)
        @Schema(description = "이미지 캡션(선택)", example = "최종 썸네일")
        private String caption;
    }
}