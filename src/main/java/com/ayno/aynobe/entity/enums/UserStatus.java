package com.ayno.aynobe.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum UserStatus {
    ACTIVE("정상"),
    BLOCKED("차단"),     // 관리자에 의한 차단
    WITHDRAWN("탈퇴");   // 사용자 자의에 의한 탈퇴

    private final String description;
}