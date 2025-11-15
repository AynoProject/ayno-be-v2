package com.ayno.aynobe.dto.workflow;

import com.ayno.aynobe.entity.enums.FlowType;
import com.ayno.aynobe.entity.enums.VisibilityType;
import com.ayno.aynobe.entity.enums.PromptType;
import com.ayno.aynobe.entity.enums.SectionType;
import com.fasterxml.jackson.databind.JsonNode;
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
@Schema(name = "WorkflowUpdateRequest")
public class WorkflowUpdateRequestDTO {

    @NotNull
    private FlowType category;

    @NotBlank @Size(max = 100)
    @Schema(description = "워크플로우 제목", example = "유튜브 썸네일 자동 생성")
    private String workflowTitle;

    private JsonNode canvasJson; // optional

    @NotEmpty @Valid
    private List<StepDTO> steps;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(name = "WorkflowUpdateStep")
    public static class StepDTO {
        /** 기존이면 값 있음, 신규면 null */
        private Long stepId;

        @Min(1)
        private int stepNo;

        @NotBlank @Size(max = 100)
        @Schema(description = "스텝 제목", example = "아이디어 정리")
        private String stepTitle;

        @NotBlank
        @Schema(description = "스텝 내용", example = "키워드 5개를 뽑고 우선순위를 정합니다.")
        private String stepContent;

        private Long toolId; // nullable

        @NotEmpty @Valid
        private List<SectionDTO> sections;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(name = "WorkflowUpdateSection")
    public static class SectionDTO {
        /** 기존이면 값 있음, 신규면 null */
        private Long sectionId;

        @Min(1)
        private int orderNo;

        @NotBlank @Size(max = 100)
        @Schema(description = "섹션 제목", example = "프롬프트 작성")
        private String sectionTitle;

        @NotNull
        @Schema(description = "섹션 타입", example = "PROMPT")
        private SectionType sectionType; // PROMPT / MEDIA / NOTE

        @NotNull
        @Schema(description = "프롬프트 역할(타입이 PROMPT일 때 의미 있음)", example = "INPUT")
        private PromptType promptRole;

        @NotBlank
        @Schema(description = "섹션 본문/프롬프트 내용", example = "You are a helpful assistant specialized in design.")
        private String stepContent;

        @Size(max = 512)
        private String baseKey;
    }
}
