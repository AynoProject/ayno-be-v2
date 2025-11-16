package com.ayno.aynobe.dto.user;

import com.ayno.aynobe.entity.JobRole;
import com.ayno.aynobe.entity.User;
import com.ayno.aynobe.entity.UserInterest;
import com.ayno.aynobe.entity.enums.AgeBand;
import com.ayno.aynobe.entity.enums.GenderType;
import com.ayno.aynobe.entity.enums.UsageDepthType;
import lombok.*;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfileResponseDTO {
    private String username;
    private String profileImageUrl;
    private GenderType gender;
    private AgeBand ageBand;
    private UsageDepthType aiUsageDepth;
    private String jobRole;
    private List<String> interests;

    public static ProfileResponseDTO from(User u) {
        return ProfileResponseDTO.builder()
                .username(u.getUsername())
                .profileImageUrl(u.getProfileImageUrl())
                .gender(u.getGender())
                .ageBand(u.getAgeBand())
                .aiUsageDepth(u.getAiUsageDepth())
                .jobRole(u.getJobRole() != null ? u.getJobRole().getJobRoleLabel() : null)
                .interests(u.getUserInterests() == null ? Collections.emptyList()
                        : u.getUserInterests().stream()
                        .map(ui -> ui.getInterest().getInterestLabel())
                        .collect(Collectors.toList()))
                .build();
    }
}
