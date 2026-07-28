package com.coderhan.lastmission.marketing.domain;

public enum BannerAdStatus {
    PENDING,   // 승인 대기
    APPROVED,  // 승인 (노출 중)
    REJECTED,  // 거절
    EXPIRED    // 기간 만료
}
