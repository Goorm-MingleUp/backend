package com.mingleup.backend.domain.application.domain;

public enum ApplicationStatus {
    PENDING,  // 승인 대기
    APPROVED, // 승인
    REJECTED, // 거절
    ATTENDED  // 참석 완료
}