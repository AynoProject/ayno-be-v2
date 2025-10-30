package com.ayno.aynobe.dto.asset;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UploadDeleteRequestDTO {
    public enum Scope { ARTIFACT, SECTION }

    @NotNull
    private Scope scope;
    private Long artifactId;     // scope=ARTIFACT일 때 필수
    private Long workflowId;     // scope=SECTION 일 때 필수
    private Long sectionId;      // scope=SECTION 일 때 필수

    @NotBlank
    @Size(max = 512)
    private String baseKey;      // presign 응답 그대로 (artifact/... or workflow/...)
}
