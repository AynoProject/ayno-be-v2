package com.ayno.aynobe.dto.asset;

import com.ayno.aynobe.entity.enums.UploadScope;
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
    private UploadScope scope;                // ARTIFACT | SECTION

    @NotBlank
    @Schema(description = "파일 확장자(마침표 제외)", example = "png")
    private String ext;

    @Positive
    @Schema(description = "파일 크기(byte)", example = "245678")
    private long bytes;
}
