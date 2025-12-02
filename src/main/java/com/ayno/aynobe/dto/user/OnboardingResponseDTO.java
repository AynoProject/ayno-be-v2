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
public class OnboardingResponseDTO {
    private Long userId;
    private GenderType gender;
    private AgeBand ageBand;
    private UsageDepthType aiUsageDepth;
    private Integer jobRoleId;
    private List<Integer> interestIds;

    public static OnboardingResponseDTO from(User u) {
        return OnboardingResponseDTO.builder()
                .userId(u.getUserId())
                .gender(u.getGender())
                .ageBand(u.getAgeBand())
                .aiUsageDepth(u.getAiUsageDepth())
                .jobRoleId(u.getJobRole() != null ? u.getJobRole().getJobRoleId() : null)
                .interestIds(
                        u.getUserInterests() == null ? List.of()
                                : u.getUserInterests().stream()
                                .map(ui -> ui.getInterest().getInterestId())
                                .toList()
                )
                .build();
    }
}
