package com.ayno.aynobe.entity.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ReportStatus {
    PENDING("처리 대기"),   // 신고 접수 후 관리자 확인 전
    RESOLVED("처리 완료"),  // 관리자가 확인하고 조치(차단/삭제 등) 취함
    REJECTED("반려");       // 허위 신고거나 문제없음으로 판단됨

    private final String description;
}
