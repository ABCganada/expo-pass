package com.coderhan.lastmission.marketing;

import java.util.UUID;

/**
 * 광고 계약 기간 만료를 알리는 도메인 이벤트.
 * payment 모듈이 이 이벤트를 구독해 광고 매출 정산을 생성한다.
 */
public record AdExpiredEvent(UUID adId, long totalAmount) {
}
