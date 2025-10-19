package com.ayno.aynobe.service;

import com.ayno.aynobe.config.exception.CustomException;
import com.ayno.aynobe.dto.common.PageResponseDTO;
import com.ayno.aynobe.dto.workflow.*;
import com.ayno.aynobe.entity.*;
import com.ayno.aynobe.entity.enums.TargetType;
import com.ayno.aynobe.repository.ReactionRepository;
import com.ayno.aynobe.repository.ToolRepository;
import com.ayno.aynobe.repository.WorkflowRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WorkflowService {

    private final WorkflowRepository workflowRepository;
    private final ReactionRepository reactionRepository;
    private final ToolRepository toolRepository;


    @Transactional(readOnly = true)
    public WorkflowDetailResponseDTO getDetail(User actorOrNull, Long workflowId) {
        Workflow wf = workflowRepository.findWithAllByWorkflowId(workflowId)
                .orElseThrow(() -> CustomException.notFound("존재하지 않는 워크플로우입니다."));

        boolean isOwner = actorOrNull != null && wf.getUser().getUserId().equals(actorOrNull.getUserId());
        if (!isOwner) {
            throw CustomException.forbidden("열람 권한이 없습니다.");
        }

        return wf.toDetailDTO();      // ← 도메인 메서드
    }

    @Transactional
    public WorkflowCreateResponseDTO create(User owner, WorkflowCreateRequestDTO requestDto) {

        validateUniqueNumbersForCreate(requestDto);
        Map<Long, Tool> toolsById = preloadToolsByIdForCreate(requestDto);

        // 3) 루트 생성
        Workflow workflow = Workflow.create(owner, requestDto);

        // 4) 스텝/섹션 추가 (도메인 메서드)
        requestDto.getSteps().forEach(stepDTO -> {
            Tool tool = null;
            if (stepDTO.getToolId() != null) {
                tool = toolsById.get(stepDTO.getToolId()); // preload 시 존재 보장
            }
            workflow.addStep(stepDTO, tool);
        });

        // 5) 저장
        Workflow saved = workflowRepository.save(workflow);
        return WorkflowCreateResponseDTO.builder()
                .workflowId(saved.getWorkflowId())
                .build();
    }

    @Transactional
    public WorkflowUpdateResponseDTO update(User actor, Long workflowId, WorkflowUpdateRequestDTO dto) {
        // 0) 로드(+user, steps, tool). sections는 SUBSELECT로 로드
        Workflow wf = workflowRepository.findWithAllByWorkflowId(workflowId)
                .orElseThrow(() -> CustomException.notFound("존재하지 않는 워크플로우입니다."));

        // 1) 권한: 오너만
        if (!wf.getUser().getUserId().equals(actor.getUserId())) {
            throw CustomException.forbidden("본인이 작성한 워크플로우만 수정할 수 있습니다.");
        }

        validateUniqueNumbersForUpdate(dto.getSteps());
        Map<Long, Tool> toolsById = preloadToolsByIdForUpdate(dto.getSteps());

        // 4) 도메인에 위임 (더티체킹)
        wf.applyHeader(dto);
        wf.syncSteps(dto.getSteps(), toolsById);

        // 5) 저장(더티체킹으로 변경분만 반영)
        Workflow saved = workflowRepository.save(wf);

        return WorkflowUpdateResponseDTO.builder()
                .workflowId(saved.getWorkflowId())
                .build();
    }

    // --------- helpers ---------

    // ---- CREATE 전용 ----
    private void validateUniqueNumbersForCreate(WorkflowCreateRequestDTO dto) {
        Set<Integer> stepNos = new HashSet<>();
        dto.getSteps().forEach(step -> {
            if (!stepNos.add(step.getStepNo())) {
                throw CustomException.duplicate("중복된 stepNo 입니다: " + step.getStepNo());
            }
            Set<Integer> orders = new HashSet<>();
            step.getSections().forEach(sec -> {
                if (!orders.add(sec.getOrderNo())) {
                    throw CustomException.duplicate(
                            "스텝 " + step.getStepNo() + " 내에서 중복된 orderNo 입니다: " + sec.getOrderNo());
                }
            });
        });
    }

    private Map<Long, Tool> preloadToolsByIdForCreate(WorkflowCreateRequestDTO dto) {
        List<Long> toolIds = dto.getSteps().stream()
                .map(WorkflowCreateRequestDTO.StepDTO::getToolId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (toolIds.isEmpty()) return Collections.emptyMap();

        Map<Long, Tool> map = toolRepository.findByToolIdIn(toolIds).stream()
                .collect(Collectors.toMap(Tool::getToolId, t -> t));

        for (Long id : toolIds) {
            if (!map.containsKey(id)) {
                throw CustomException.notFound("존재하지 않는 Tool ID: " + id);
            }
        }
        return map;
    }

    // ---- UPDATE 전용 ----
    private void validateUniqueNumbersForUpdate(List<WorkflowUpdateRequestDTO.StepDTO> steps) {
        Set<Integer> stepNos = new HashSet<>();
        for (var s : steps) {
            if (!stepNos.add(s.getStepNo())) {
                throw CustomException.duplicate("중복된 stepNo 입니다: " + s.getStepNo());
            }
            Set<Integer> orders = new HashSet<>();
            for (var sec : s.getSections()) {
                if (!orders.add(sec.getOrderNo())) {
                    throw CustomException.duplicate("스텝 " + s.getStepNo() + " 내에서 중복된 orderNo 입니다: " + sec.getOrderNo());
                }
            }
        }
    }

    private Map<Long, Tool> preloadToolsByIdForUpdate(List<WorkflowUpdateRequestDTO.StepDTO> steps) {
        List<Long> ids = steps.stream()
                .map(WorkflowUpdateRequestDTO.StepDTO::getToolId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (ids.isEmpty()) return Collections.emptyMap();

        Map<Long, Tool> map = toolRepository.findByToolIdIn(ids).stream()
                .collect(Collectors.toMap(Tool::getToolId, t -> t));

        for (Long id : ids) {
            if (!map.containsKey(id)) {
                throw CustomException.notFound("존재하지 않는 Tool ID: " + id);
            }
        }
        return map;
    }

    @Transactional
    public WorkflowDeleteResponseDTO delete(User actor, Long workflowId) {
        // 1) 로드(+작성자)
        Workflow workflow = workflowRepository.findByWorkflowId(workflowId)
                .orElseThrow(() -> CustomException.notFound("존재하지 않는 워크플로우입니다."));

        // 2) 권한: 작성자만 허용
        boolean isOwner = workflow.getUser().getUserId().equals(actor.getUserId());
        if (!isOwner) {
            throw CustomException.forbidden("본인이 작성한 워크플로우만 삭제할 수 있습니다.");
        }

        // 3) Reaction 정리 (타깃: WORKFLOW)
        reactionRepository.deleteByTargetTypeAndTargetId(TargetType.WORKFLOW, workflowId);

        // 4) 루트 삭제 (cascade로 step/section 함께 제거)
        workflowRepository.delete(workflow);

        return WorkflowDeleteResponseDTO.builder()
                .workflowId(workflowId)
                .build();
    }
}
