package com.ayno.aynobe.repository;

import com.ayno.aynobe.entity.StepSection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

public interface StepSectionRepository extends JpaRepository<StepSection, Long> {
    @Query("SELECT ss.baseKey FROM StepSection ss WHERE ss.baseKey IN :baseKeys")
    Set<String> findBaseKeysIn(@Param("baseKeys") List<String> baseKeys);

    @Query("SELECT ss.baseKey FROM StepSection ss " +
            "WHERE ss.workflowStep.workflow.workflowId = :workflowId " +
            "AND ss.baseKey IS NOT NULL")
    List<String> findAllBaseKeysByWorkflowId(@Param("workflowId") Long workflowId);
}
