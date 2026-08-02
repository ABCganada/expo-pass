package com.coderhan.lastmission.shared.event;

import java.time.OffsetDateTime;
import com.coderhan.lastmission.shared.order.OrderType;

/**
 * payment 모듈이 결제 승인 성공 직후 발행하는 도메인 이벤트.
 * reservation/marketing이 각자 @ApplicationModuleListener로 구독해 PENDING 주문을 갱신한다.
 *
 * shared에 두는 이유: reservation/marketing이 이 이벤트 타입을 참조하려면 payment에 의존해야 하는데,
 * payment가 이미 reservation/marketing에 의존하고 있어(주문 금액 조회) 순환 의존이 되기 때문.
 */
public record PaymentConfirmedEvent(
        long paymentId,
        String orderId,
        OrderType orderType,
        OffsetDateTime confirmedAt
) {
}
