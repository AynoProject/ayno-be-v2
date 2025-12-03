package com.ayno.aynobe.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReportTargetType {
    USER("사용자"),       // 악성 유저 신고
    ARTIFACT("결과물"),   // 부적절한 이미지/음악 신고
    COMMENT("댓글");      // (추후 확장) 비방 댓글 신고

    private final String description;
}
