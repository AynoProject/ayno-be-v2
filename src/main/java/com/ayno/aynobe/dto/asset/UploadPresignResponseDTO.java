package com.ayno.aynobe.dto.asset;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(name = "UploadPresignResponse")
public class UploadPresignResponseDTO {

    @NotBlank
    @Schema(description = "S3 상대 경로(baseKey)", example = "artifact/123/media/uuid/original.jpg")
    private String baseKey;

    @NotBlank
    @Schema(description = "S3 Presigned PUT URL")
    private String putUrl;

    @NotBlank
    @Schema(description = "Content-Type", example = "image/jpeg")
    private String contentType;
}
