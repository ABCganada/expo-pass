package com.coderhan.lastmission.shared.event;

import java.time.OffsetDateTime;
import com.coderhan.lastmission.shared.order.OrderType;

/**
 * payment 모듈이 결제 실패/취소 신고 시 발행하는 도메인 이벤트.
 * reservation/marketing이 각자 @ApplicationModuleListener로 구독해 PENDING 주문을 갱신한다.
 * 
 * shared에 두는 이유는 PaymentConfirmedEvent와 동일(순환 의존 회피).
 */
public record PaymentFailedEvent(
        String orderId,
        OrderType orderType,
        Long userId,
        String reason,
        OffsetDateTime failedAt
) {
}
