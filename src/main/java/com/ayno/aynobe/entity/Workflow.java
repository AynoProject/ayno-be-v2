package com.ayno.aynobe.entity;

import com.ayno.aynobe.dto.workflow.WorkflowCreateRequestDTO;
import com.ayno.aynobe.dto.workflow.WorkflowDetailResponseDTO;
import com.ayno.aynobe.entity.enums.FlowType;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Entity
@Table(name = "workflow",
        indexes = {
                @Index(name = "idx_workflow_owner", columnList = "userId"),
                @Index(name = "idx_workflow_rank",  columnList = "likeCount, viewCount")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Workflow extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long workflowId;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 30)
    private FlowType category;

    @Column(name = "workflowTitle", nullable = false, length = 100)
    private String workflowTitle;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "userId", nullable = false)
    private User user;

    // JSON
    @JdbcTypeCode(SqlTypes.JSON)             // Hibernate 6
    @Column(name = "canvasJson", columnDefinition = "json")
    private JsonNode canvasJson;

    @Builder.Default
    @OneToMany(mappedBy = "workflow", cascade = CascadeType.ALL, orphanRemoval = true)
    @Fetch(FetchMode.SUBSELECT)
    private List<WorkflowStep> workflowSteps = new ArrayList<>();

    // 디테일용 DTO (스텝/섹션 정렬 포함)
    public WorkflowDetailResponseDTO toDetailDTO() {
        return WorkflowDetailResponseDTO.builder()
                .workflowId(this.workflowId)
                .category(this.category)
                .workflowTitle(this.workflowTitle)
                .canvasJson(this.canvasJson)
                .ownerId(this.user.getUserId())
                .ownerName(this.user.getUsername())
                .steps(this.workflowSteps.stream()
                        .sorted(Comparator.comparingInt(WorkflowStep::getStepNo))
                        .map(WorkflowStep::toDetailDTO)
                        .collect(Collectors.toList()))
                .build();
    }

    /** 루트 생성 팩토리 (기본 필드만 세팅) */
    public static Workflow create(User owner, WorkflowCreateRequestDTO dto) {
        return Workflow.builder()
                .category(dto.getCategory())
                .workflowTitle(dto.getWorkflowTitle())
                .user(owner)
                .canvasJson(dto.getCanvasJson())
                .build();
    }

    /** 스텝 + 섹션들을 DTO 기반으로 추가 (Tool은 이미 조회된 객체 or null) */
    public WorkflowStep addStep(WorkflowCreateRequestDTO.StepDTO stepDTO, Tool toolOrNull) {
        WorkflowStep step = WorkflowStep.builder()
                .workflow(this)
                .stepNo(stepDTO.getStepNo())
                .stepTitle(stepDTO.getStepTitle())
                .stepContent(stepDTO.getStepContent())
                .tool(toolOrNull)
                .build();

        // 섹션 추가
        if (stepDTO.getSections() != null) {
            stepDTO.getSections().forEach(secDTO -> step.addSection(secDTO));
        }

        this.workflowSteps.add(step);
        return step;
    }

    /** 헤더(단순 필드) 더티체킹 */
    public void applyHeader(com.ayno.aynobe.dto.workflow.WorkflowUpdateRequestDTO dto) {
        this.category = dto.getCategory();
        this.workflowTitle = dto.getWorkflowTitle();
        this.canvasJson = dto.getCanvasJson();
    }

    /** steps diff 동기화 (추가/수정/삭제 + 재정렬) */
    public void syncSteps(
            List<com.ayno.aynobe.dto.workflow.WorkflowUpdateRequestDTO.StepDTO> newSteps,
            Map<Long, Tool> toolsById
    ) {
        // 기존 step 맵
        Map<Long, WorkflowStep> existed = this.workflowSteps.stream()
                .collect(java.util.stream.Collectors.toMap(WorkflowStep::getStepId, s -> s));

        List<WorkflowStep> next = new ArrayList<>(newSteps.size());

        for (var sDto : newSteps) {
            WorkflowStep step;
            if (sDto.getStepId() != null && existed.containsKey(sDto.getStepId())) {
                // 수정
                step = existed.remove(sDto.getStepId());
                step.setStepNo(sDto.getStepNo());
                step.setStepTitle(sDto.getStepTitle());
                step.setStepContent(sDto.getStepContent());
                step.setTool(sDto.getToolId() != null ? toolsById.get(sDto.getToolId()) : null);
            } else {
                // 추가
                step = WorkflowStep.builder()
                        .workflow(this)
                        .stepNo(sDto.getStepNo())
                        .stepTitle(sDto.getStepTitle())
                        .stepContent(sDto.getStepContent())
                        .tool(sDto.getToolId() != null ? toolsById.get(sDto.getToolId()) : null)
                        .build();
            }

            // 자식 섹션 동기화
            step.syncSections(sDto.getSections());

            next.add(step);
        }

        // 삭제: existed 에 남은 step 들
        for (WorkflowStep removed : existed.values()) {
            removed.detachAllSections();
            removed.setWorkflow(null);             // orphanRemoval=true → DELETE
        }

        // 교체(참조 유지) — next로 재정렬 반영
        this.workflowSteps.clear();
        this.workflowSteps.addAll(next);
    }
}

