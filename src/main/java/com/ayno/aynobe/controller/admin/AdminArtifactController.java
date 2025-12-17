package com.ayno.aynobe.controller.admin;

import com.ayno.aynobe.dto.admin.AdminArtifactResponseDTO;
import com.ayno.aynobe.dto.common.PageResponseDTO;
import com.ayno.aynobe.dto.common.Response;
import com.ayno.aynobe.entity.enums.VisibilityType;
import com.ayno.aynobe.service.admin.AdminArtifactService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "AdminArtifact", description = "관리자 결과물 관리 API")
@RestController
@RequestMapping("/api/admin/artifacts")
@RequiredArgsConstructor
public class AdminArtifactController {
    private final AdminArtifactService adminArtifactService;

    @Operation(summary = "관리자 프로젝트 목록 조회", description = "ID검색, 상태필터, 제목/작성자 검색 통합")
    @GetMapping
    public ResponseEntity<Response<PageResponseDTO<AdminArtifactResponseDTO>>> getArtifacts(
            @RequestParam(required = false) VisibilityType status,
            @RequestParam(required = false, name = "q") String keyword, // 검색어 (ID or 텍스트)
            @ParameterObject Pageable pageable
    ) {
        return ResponseEntity.ok(Response.success(adminArtifactService.getArtifacts(status, keyword, pageable)));
    }

    @Operation(summary = "프로젝트 강제 삭제", description = "관리자 권한으로 DB에서 완전히 삭제합니다. (신고 내역도 같이 삭제됨)")
    @DeleteMapping("/{artifactId}")
    public ResponseEntity<?> deleteArtifact(
            @PathVariable Long artifactId
    ) {
        adminArtifactService.deleteArtifact(artifactId);
        return ResponseEntity.ok(Response.success("삭제되었습니다"));
    }

}
