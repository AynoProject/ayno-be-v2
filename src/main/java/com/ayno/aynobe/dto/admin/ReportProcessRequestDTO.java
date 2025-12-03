package com.ayno.aynobe.dto.admin;

import com.ayno.aynobe.entity.enums.ReportStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class ReportProcessRequestDTO {

    @NotNull(message = "처리 상태는 필수입니다.")
    @Schema(description = "변경할 상태 (RESOLVED: 처리완료, REJECTED: 반려)", example = "RESOLVED")
    private ReportStatus status;

    @Schema(description = "관리자 처리 메모 (사유 등)", example = "욕설 확인되어 차단 조치함")
    private String memo;
}
