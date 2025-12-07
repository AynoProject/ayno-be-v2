package com.ayno.aynobe.controller.admin;

import com.ayno.aynobe.dto.admin.AdminUserResponseDTO;
import com.ayno.aynobe.dto.admin.UserStatusUpdateDTO;
import com.ayno.aynobe.dto.common.PageResponseDTO;
import com.ayno.aynobe.dto.common.Response;
import com.ayno.aynobe.entity.enums.UserStatus;
import com.ayno.aynobe.service.admin.AdminUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@Tag(name = "AdminUser", description = "관리자 유저 관리 API")
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    @Operation(summary = "유저 검색", description = "검색, 상태 필터, 기간 조회 지원")
    @GetMapping
    public ResponseEntity<Response<PageResponseDTO<AdminUserResponseDTO>>> getUsers(
            @RequestParam(required = false) UserStatus status,
            @RequestParam(required = false, name = "q") String keyword,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @ParameterObject Pageable  pageable
    ){
        return ResponseEntity.ok(Response.success(adminUserService.getUsers(status, keyword, from, to, pageable)));
    }

    @Operation(summary = "유저 상태 변경", description = "회원을 차단하거나 정상상태로 변경")
    @PatchMapping("/{userId}/status")
    public ResponseEntity<?> changeStatus(
            @PathVariable Long userId,
            @RequestBody UserStatusUpdateDTO userStatusUpdateDTO
    ) {
        adminUserService.changeUserStatus(userId, userStatusUpdateDTO.getStatus());
        return ResponseEntity.ok(Response.success("상태가 변경되었습니다."));
    }
}
