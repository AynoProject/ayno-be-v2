package com.ayno.aynobe.entity;

import com.ayno.aynobe.dto.workflow.WorkflowDetailResponseDTO;
import com.ayno.aynobe.entity.enums.PromptType;
import com.ayno.aynobe.entity.enums.SectionType;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "stepSection",
        indexes = {
                @Index(name = "idx_section_step", columnList = "stepId, orderNo"),
                @Index(name = "idx_section_kind", columnList = "stepType, promptRole")
        })
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StepSection extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long sectionId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "stepId", nullable = false)
    private WorkflowStep workflowStep;

    @Column(name = "orderNo", nullable = false)
    private int orderNo;

    @Column(name = "sectionTitle", nullable = false, length = 100)
    private String sectionTitle;

    @Enumerated(EnumType.STRING)
    @Column(name = "sectionType", nullable = false, length = 20)
    private SectionType sectionType;   // PROMPT / MEDIA / NOTE

    @Enumerated(EnumType.STRING)
    @Column(name = "promptRole", nullable = false, length = 20)
    private PromptType promptRole; // PROMPT일 때 의미 있음

    @Column(name = "stepContent", nullable = false, columnDefinition = "TEXT")
    private String stepContent;

    @Column(name = "baseKey", length = 512)
    private String baseKey;

    public WorkflowDetailResponseDTO.SectionDTO toDetailDTO() {
        return WorkflowDetailResponseDTO.SectionDTO.builder()
                .sectionId(this.sectionId)
                .orderNo(this.orderNo)
                .sectionTitle(this.sectionTitle)
                .stepType(this.sectionType.name())
                .promptRole(this.promptRole.name())
                .stepContent(this.stepContent)
                .mediaUrl(this.baseKey)
                .build();
    }
}

