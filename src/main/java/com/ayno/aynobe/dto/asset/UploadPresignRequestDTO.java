package com.ayno.aynobe.dto.asset;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(name = "UploadPresignRequest")
public class UploadPresignRequestDTO {

    @NotNull
    @Schema(description = "업로드 범위", example = "ARTIFACT")
    private Scope scope;                // ARTIFACT | SECTION

    @Schema(description = "scope=ARTIFACT일 때 필수")
    private Long artifactId;            // ARTIFACT용

    @Schema(description = "scope=SECTION일 때 필수")
    private Long workflowId;            // SECTION용

    @Schema(description = "scope=SECTION일 때 필수")
    private Long sectionId;             // SECTION용

    @NotBlank
    @Schema(description = "파일 확장자(마침표 제외)", example = "jpg")
    private String ext;

    @Positive
    @Schema(description = "파일 크기(byte)", example = "245678")
    private long bytes;

    /** 조건부 검증: scope에 맞는 ID가 왔는지 */
    @AssertTrue(message = "scope=ARTIFACT면 artifactId가, scope=SECTION이면 workflowId와 sectionId가 필요합니다.")
    public boolean isScopeIdsValid() {
        if (scope == null) return true;
        return switch (scope) {
            case ARTIFACT -> artifactId != null;
            case SECTION  -> workflowId != null && sectionId != null;
        };
    }

    public enum Scope { ARTIFACT, SECTION }
}
