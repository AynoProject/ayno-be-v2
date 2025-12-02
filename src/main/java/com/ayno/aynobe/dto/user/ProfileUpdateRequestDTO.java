package com.ayno.aynobe.dto.user;

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
public class ProfileUpdateRequestDTO {
    private String nickname;

    private Integer jobRoleId;       // 선택된 직무 ID
    private List<Integer> interestIds; // 선택된 관심사 ID 목록 (최대 3개)

    private GenderType gender;
    private AgeBand ageBand;
    private UsageDepthType aiUsageDepth;

     private String profileImageBaseKey;
}
