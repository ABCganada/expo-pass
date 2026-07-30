package com.coderhan.lastmission.marketing.domain;

public enum BannerAdStatus {
    PENDING,   // 결제 완료, 관리자 승인 대기
    APPROVED,  // 관리자 승인 완료 (노출 중)
    REJECTED,  // 거절
    EXPIRED    // 기간 만료
}
