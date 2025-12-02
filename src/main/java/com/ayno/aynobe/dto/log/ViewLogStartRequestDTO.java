package com.ayno.aynobe.dto.log;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 조회 시작(진입) 요청 DTO
 * - artifactId 기준으로 (userId, visitDate) 하루 1회 upsert
 * - enteredAt 미지정 시 서버 시각 사용
 */
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ViewLogCreateRequestDTO {

    @Schema(description = "결과물 ID", example = "123")
    @NotNull
    private Long artifactId;
}