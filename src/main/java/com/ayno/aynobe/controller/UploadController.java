package com.ayno.aynobe.controller;

import com.ayno.aynobe.config.security.CustomUserDetails;
import com.ayno.aynobe.dto.asset.UploadDeleteRequestDTO;
import com.ayno.aynobe.dto.asset.UploadPresignRequestDTO;
import com.ayno.aynobe.dto.asset.UploadPresignResponseDTO;
import com.ayno.aynobe.service.UploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Upload", description = "파일 업로드 관련 API")
@RestController
@RequiredArgsConstructor
public class UploadController {
    private final UploadService uploadService;

    @Operation(
            summary = "이미지 업로드 presign",
            description = "s3에 이미지 업로드 presign")
    @PostMapping("/api/uploads/presign")
    public ResponseEntity<UploadPresignResponseDTO> presign(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestBody @Valid UploadPresignRequestDTO req
    ) {
        return ResponseEntity.ok(uploadService.createPresign(principal.getUser(), req));
    }

    @Operation(
            summary = "작성 중 업로드 취소(즉시 삭제)",
            description = "작성 화면에서 올렸다가 뺀 파일을 S3 private 경로에서 즉시 삭제합니다. baseKey는 presign 응답의 값을 그대로 사용하세요."
    )
    @DeleteMapping("/api/uploads/object")
    public ResponseEntity<Void> deleteObject(
            @AuthenticationPrincipal CustomUserDetails principal,
            @RequestBody @Valid UploadDeleteRequestDTO req
    ) {
        uploadService.deletePrivateObject(principal.getUser(), req);
        return ResponseEntity.noContent().build();
    }
}
