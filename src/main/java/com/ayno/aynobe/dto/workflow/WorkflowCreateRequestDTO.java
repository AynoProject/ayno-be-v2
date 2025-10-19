package com.ayno.aynobe.dto.workflow;

import com.ayno.aynobe.entity.enums.FlowType;
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
@Schema(name = "WorkflowCreateRequest")
public class WorkflowCreateRequestDTO {

    @NotNull
    private FlowType category;

    @NotBlank @Size(max = 100)
    private String workflowTitle;

    @Schema(description = "캔버스 JSON 객체 예: {\"nodes\":[],\"edges\":[]}")
    private JsonNode canvasJson; // optional

    @NotEmpty @Valid
    private List<WorkflowCreateRequestDTO.StepDTO> steps;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(name = "WorkflowCreateStep")
    public static class StepDTO {
        @Min(1)
        private int stepNo;

        @NotBlank @Size(max = 100)
        private String stepTitle;

        @NotBlank
        private String stepContent;

        @Schema(description = "사용할 툴 ID, 없으면 null")
        private Long toolId; // nullable

        @NotEmpty @Valid
        private List<WorkflowCreateRequestDTO.SectionDTO> sections;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    @Schema(name = "WorkflowCreateSection")
    public static class SectionDTO {
        @Min(1)
        private int orderNo;

        @NotBlank @Size(max = 100)
        private String sectionTitle;

        @NotNull
        private SectionType sectionType; // PROMPT / MEDIA / NOTE

        @NotNull
        private PromptType promptRole;   // PROMPT일 때 의미 있음

        @NotBlank
        private String stepContent;

        @Size(max = 512)
        private String baseKey;
    }
}