package com.ayno.aynobe.dto.report;

import com.ayno.aynobe.entity.enums.ReportTargetType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ReportCreateRequestDTO {

    @NotNull(message = "신고 대상 ID는 필수입니다.")
    @Schema(description = "신고할 대상의 ID (유저ID or 결과물ID)", example = "1")
    private Long targetId;

    @NotNull(message = "신고 대상 타입은 필수입니다.")
    @Schema(description = "신고 대상 종류 (USER, ARTIFACT)", example = "ARTIFACT")
    private ReportTargetType targetType;

    @NotBlank(message = "신고 사유를 입력해주세요.")
    @Schema(description = "신고 사유 상세", example = "부적절한 이미지가 포함되어 있습니다.")
    private String reason;
}
