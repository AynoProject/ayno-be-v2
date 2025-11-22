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

    // 2. 직무 및 관심사 (ID로 받음)
    private Integer jobRoleId;       // 선택된 직무 ID
    private List<Integer> interestIds; // 선택된 관심사 ID 목록 (최대 3개)

    // 3. 부가 정보 (온보딩 수정 대응)
    private GenderType gender;
    private AgeBand ageBand;
    private UsageDepthType aiUsageDepth;

    // 프로필 이미지는 추후 S3 연동 시 추가
    // private String profileImageUrl;
}
