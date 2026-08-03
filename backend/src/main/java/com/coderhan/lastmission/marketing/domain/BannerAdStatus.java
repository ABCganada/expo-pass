package com.coderhan.lastmission.marketing.domain;

public enum BannerAdStatus {
    PENDING,    // 등록 완료, 결제 대기 중
    APPROVED,   // 결제 완료, 노출 중
    REJECTED,   // 관리자 거절
    EXPIRED,    // 기간 만료
    CANCELLED,  // 미결제 타임아웃 취소
    REFUNDED    // 결제 후 환불됨
}
