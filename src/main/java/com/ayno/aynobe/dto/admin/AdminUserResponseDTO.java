package com.ayno.aynobe.dto.admin;

import com.ayno.aynobe.entity.User;
import com.ayno.aynobe.entity.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminUserResponseDTO {
    private Long userId;
    private String email;      // username을 email로 사용 중이라면 매핑
    private String nickname;
    private UserStatus status;
    private LocalDateTime joinedAt; // 가입일 (BaseTimeEntity의 createdAt)
    private Long reportedCount; // (나중에 신고 기능 붙일 때 추가 고려, 지금은 0)

    public static AdminUserResponseDTO from(User user) {
        return AdminUserResponseDTO.builder()
                .userId(user.getUserId())
                .email(user.getUsername())
                .nickname(user.getNickname())
                .status(user.getStatus())
                .joinedAt(user.getCreatedAt())
                .reportedCount(0L) // 추후 구현
                .build();
    }
}
