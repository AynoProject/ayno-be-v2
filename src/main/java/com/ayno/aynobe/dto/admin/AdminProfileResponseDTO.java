package com.ayno.aynobe.dto.admin;

import com.ayno.aynobe.entity.Admin;
import com.ayno.aynobe.entity.enums.AdminRole;
import com.ayno.aynobe.entity.enums.AdminStatus;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminProfileResponseDTO {
    private Long adminId;
    private String adminName;
    private AdminRole role;      // 프론트엔드 메뉴/버튼 노출 제어용
    private AdminStatus status;  // 계정 활성 상태 확인용

    public static AdminProfileResponseDTO from(Admin admin) {
        return AdminProfileResponseDTO.builder()
                .adminId(admin.getAdminId())
                .adminName(admin.getAdminName())
                .role(admin.getRole())
                .status(admin.getStatus())
                .build();
    }
}
