package com.ayno.aynobe.repository;

import com.ayno.aynobe.entity.StepSection;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StepSectionRepository extends JpaRepository<StepSection, Long> {
    boolean existsBySectionIdAndWorkflowStep_Workflow_WorkflowId(Long sectionId, Long workflowId);
}
