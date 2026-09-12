package com.coderhan.lastmission.marketing;

import java.util.UUID;

/**
 * 관리자가 결제완료(PAID) 상태의 광고를 반려했음을 알리는 도메인 이벤트.
 * payment 모듈이 이 이벤트를 구독해 결제를 자동 환불한다(광고 환불 정책 — 항상 100%).
 */
public record AdRejectedEvent(UUID adId, String orderId) {
}
