package com.ayno.aynobe.controller;

import com.ayno.aynobe.config.security.CustomUserDetails;
import com.ayno.aynobe.dto.common.PageResponseDTO;
import com.ayno.aynobe.dto.common.Response;
import com.ayno.aynobe.dto.workflow.*;
import com.ayno.aynobe.entity.User;
import com.ayno.aynobe.service.WorkflowService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Workflow", description = "워크플로우 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/workflows")
public class WorkflowController {

    private final WorkflowService workflowService;

    @Operation(
            summary = "워크플로우 상세"
    )
    @GetMapping("/{workflowId}")
    public ResponseEntity<Response<WorkflowDetailResponseDTO>> getDetail(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long workflowId
    ) {
        User actor = (principal != null) ? principal.getUser() : null;
        WorkflowDetailResponseDTO body = workflowService.getDetail(actor, workflowId);
        return ResponseEntity.ok(Response.success(body));
    }


    @Operation(
            summary = "워크플로우 생성"
    )
    @PostMapping
    public ResponseEntity<Response<WorkflowCreateResponseDTO>> create(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody WorkflowCreateRequestDTO requestDTO
    ) {
        User owner = principal.getUser();
        WorkflowCreateResponseDTO res = workflowService.create(owner, requestDTO);
        return ResponseEntity.ok(Response.success(res));
    }


    @Operation(
            summary = "워크플로우 수정"
    )
    @PutMapping("/{workflowId}")
    public ResponseEntity<Response<WorkflowUpdateResponseDTO>> update(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long workflowId,
            @Valid @RequestBody WorkflowUpdateRequestDTO requestDTO
    ) {
        User actor = principal.getUser();
        var res = workflowService.update(actor, workflowId, requestDTO);
        return ResponseEntity.ok(Response.success(res));
    }


    @Operation(
            summary = "워크플로우 삭제(고아 객체 롤백용)"
    )
    @DeleteMapping("/{workflowId}")
    public ResponseEntity<Response<WorkflowDeleteResponseDTO>> delete(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long workflowId
    ) {
        User actor = principal.getUser();
        WorkflowDeleteResponseDTO res = workflowService.delete(actor, workflowId);
        return ResponseEntity.ok(Response.success(res));
    }
}
