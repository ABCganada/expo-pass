package com.coderhan.lastmission.marketing.domain;

public enum BannerAdStatus {
    PENDING,    // 승인 대기
    CONFIRMED,  // 어드민 수락 완료, 결제 대기
    APPROVED,   // 결제 완료 (노출 중)
    REJECTED,   // 거절
    EXPIRED     // 기간 만료
}
