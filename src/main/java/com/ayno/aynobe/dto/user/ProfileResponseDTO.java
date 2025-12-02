package com.ayno.aynobe.dto.user;

import com.ayno.aynobe.entity.User;
import com.ayno.aynobe.entity.enums.AgeBand;
import com.ayno.aynobe.entity.enums.GenderType;
import com.ayno.aynobe.entity.enums.UsageDepthType;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileResponseDTO {
    private Long userId;
    private String username;
    private String nickname;
    private String profileImageUrl; // 지금은 null
    private String jobRoleName;

    private List<String> interests;

    private GenderType gender;
    private AgeBand ageBand;
    private UsageDepthType aiUsageDepth;

    // User 엔티티 -> DTO 변환 메서드
    public static ProfileResponseDTO from(User user) {
        return ProfileResponseDTO.builder()
                .userId(user.getUserId())
                .username(user.getUsername())
                .nickname(user.getNickname())
                .profileImageUrl(user.getProfileImageUrl())
                .jobRoleName(user.getJobRole() != null ? user.getJobRole().getJobRoleLabel() : null) // 필드명 확인 필요
                .interests(user.getUserInterests().stream()
                        .map(ui -> ui.getInterest().getInterestLabel()) // 필드명 확인 필요
                        .toList())
                .gender(user.getGender())
                .ageBand(user.getAgeBand())
                .aiUsageDepth(user.getAiUsageDepth())
                .build();
    }
}
