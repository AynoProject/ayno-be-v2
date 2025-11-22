package com.ayno.aynobe.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Schema(description = "회원가입 요청 DTO")
public class SignUpRequestDTO {
    @Schema(description = "아이디", example = "testuser", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "아이디는 필수 입력 값입니다.")
    private String username;

    @Schema(description = "비밀번호", example = "testpass", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "비밀번호는 필수 입력 값입니다.")
    private String password;
}