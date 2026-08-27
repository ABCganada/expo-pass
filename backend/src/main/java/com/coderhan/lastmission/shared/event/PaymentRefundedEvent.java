package com.coderhan.lastmission.shared.event;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import com.coderhan.lastmission.shared.order.OrderType;

/**
 * payment 모듈이 환불 처리 직후 발행하는 도메인 이벤트.
 * reservation/marketing이 각자 @ApplicationModuleListener로 구독해 CONFIRMED/APPROVED 상태인
 * 주문을 취소 처리해야 한다(그래야 환불된 결제로 QR 입장 등이 계속 유효해지는 걸 막을 수 있다).
 *
 * shared에 두는 이유는 PaymentConfirmedEvent와 동일(순환 의존 회피).
 */
public record PaymentRefundedEvent(
        String orderId,
        OrderType orderType,
        long paymentId,
        BigDecimal refundAmount,
        OffsetDateTime refundedAt
) {
}
