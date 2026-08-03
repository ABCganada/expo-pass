package com.coderhan.lastmission.marketing.domain;

public enum BannerAdStatus {
    PENDING,    // 등록 완료, 결제 대기 중
    PAID,       // 결제 완료, 관리자 검토 대기 중
    APPROVED,   // 관리자 승인, 노출 중
    REJECTED,   // 관리자 거절
    EXPIRED,    // 기간 만료
    CANCELLED   // 미결제 타임아웃 취소
}
