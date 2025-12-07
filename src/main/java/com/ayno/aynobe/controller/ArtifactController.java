package com.ayno.aynobe.controller;

import com.ayno.aynobe.config.security.CustomUserDetails;
import com.ayno.aynobe.dto.artifact.*;
import com.ayno.aynobe.dto.asset.ArtifactPublishResponseDTO;
import com.ayno.aynobe.dto.common.PageResponseDTO;
import com.ayno.aynobe.dto.common.Response;
import com.ayno.aynobe.entity.enums.FlowType;
import com.ayno.aynobe.service.ArtifactService;
import com.ayno.aynobe.service.s3.PublishService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Artifact", description = "결과물 관련 API")
@RestController
@RequestMapping("/api/artifacts")
@RequiredArgsConstructor
public class ArtifactController {

    private final ArtifactService artifactService;
    private final PublishService publishService;

    @Operation(
            summary = "공개 결과물 목록 조회",
            description = "visibility=PUBLIC 결과물을 최신순으로 페이지네이션하여 반환")
    @GetMapping
    public ResponseEntity<Response<PageResponseDTO<ArtifactListItemResponseDTO>>> list(
            @RequestParam(required = false) FlowType category,
            @RequestParam(defaultValue = "0") int page,   // 0-base
            @RequestParam(defaultValue = "12") int size,
            @RequestParam(defaultValue = "createdAt,desc") String sort // createdAt|likeCount|viewCount
    ) {
        return ResponseEntity.ok(Response.success(
                artifactService.listPublic(category, page, size, sort)
        ));
    }

    @Operation(
            summary = "결과물 상세 조회",
            description = "미디어(이미지/영상/파일) 포함")
    @GetMapping("/{artifactId}")
    public ResponseEntity<Response<ArtifactDetailResponseDTO>> detail(
            @PathVariable Long artifactId
    ) {
        return ResponseEntity.ok(Response.success(artifactService.getDetail(artifactId)));
    }

    @Operation(
            summary = "결과물 등록",
            description = "썸네일/미디어/워크플로우(선택) 포함하여 결과물을 등록합니다.")
    @PostMapping
    public ResponseEntity<Response<ArtifactCreateResponseDTO>> create(
            @AuthenticationPrincipal CustomUserDetails principal,
            @Valid @RequestBody ArtifactCreateRequestDTO request
    ) {
        ArtifactCreateResponseDTO result = artifactService.create(principal.getUser(), request);
        return ResponseEntity.ok(Response.success(result));
    }

    @Operation(summary = "결과물 수정", description = "썸네일/미디어/워크플로우(선택) 포함하여 결과물을 수정합니다. (미디어는 전체 교체)")
    @PutMapping("/{artifactId}")
    public ResponseEntity<Response<ArtifactUpdateResponseDTO>> update(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long artifactId,
            @Valid @RequestBody ArtifactUpdateRequestDTO request
    ) {
        ArtifactUpdateResponseDTO result =
                artifactService.update(principal.getUser(), artifactId, request);
        return ResponseEntity.ok(Response.success(result));
    }

    @Operation(
            summary = "결과물 삭제",
            description = "본인이 등록한 결과물을 삭제합니다.")
    @DeleteMapping("/{artifactId}")
    public ResponseEntity<Response<ArtifactDeleteResponseDTO>> delete(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable Long artifactId
    ) {
        var result = artifactService.delete(principal.getUser(), artifactId);
        return ResponseEntity.ok(Response.success(result));
    }

    @Operation(
            summary = "결과물 공개",
            description = "본인이 등록한 결과물을 public으로 발행합니다.")
    @PutMapping("/{artifactId}/publish")
    public ResponseEntity<ArtifactPublishResponseDTO> publish(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable long artifactId
    ) {
        var res = publishService.publishArtifact(principal.getUser(), artifactId);
        return ResponseEntity.ok(res);
    }

    @Operation(
            summary = "결과물 미공개",
            description = "본인이 등록한 결과물을 unpublic으로 발행합니다.")
    @PutMapping("/{artifactId}/unpublish")
    public ResponseEntity<ArtifactPublishResponseDTO> unpublish(
            @AuthenticationPrincipal CustomUserDetails principal,
            @PathVariable long artifactId
    ) {
        var res = publishService.unpublishArtifact(principal.getUser(), artifactId);
        return ResponseEntity.ok(res);
    }

    @Operation(summary = "메인 리스트 및 검색", description = "공개된(PUBLIC) 결과물만 조회합니다. 카테고리가 없으면 전체 조회입니다.")
    @GetMapping("/search")
    public ResponseEntity<Response<PageResponseDTO<ArtifactListItemResponseDTO>>> getArtifacts(
            @RequestParam(required = false) FlowType category, // null이면 전체(All)
            @RequestParam(required = false, name = "q") String keyword,
            @RequestParam(required = false, defaultValue = "createdAt") String sort,
            @ParameterObject Pageable pageable
    ) {
        return ResponseEntity.ok(
                Response.success(artifactService.searchPublicArtifacts(category, keyword, sort, pageable))
        );
    }

}