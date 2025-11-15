package com.ayno.aynobe.dto.user;

import com.ayno.aynobe.entity.enums.AgeBand;
import com.ayno.aynobe.entity.enums.GenderType;
import com.ayno.aynobe.entity.enums.UsageDepthType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "내 온보딩 정보 DTO")
public class OnboardingUpsertRequestDTO {
    @Schema(description="성별(null이면 변경 안 함)", example="MALE")
    private GenderType gender;

    @Schema(description="연령대(null이면 변경 안 함)", example="TEEN")
    private AgeBand ageBand;

    @Schema(description="AI 사용깊이(null이면 변경 안 함)", example="INTERMEDIATE")
    private UsageDepthType usageDepth;

    @Schema(description="직무 ID(null이면 변경 안 함)", example="1")
    private Integer jobRoleId;

    @Schema(description="관심요소 ID 목록(null=미변경, []=전체 해제, [ids]=교체(최대3))", example="[1,2,3]")
    @Size(max = 3, message = "관심분야는 최대 3개까지 선택 가능합니다.")
    private List<Integer> interestIds;
}
