package com.coderhan.lastmission.reservation.application;

import com.coderhan.lastmission.shared.event.PaymentRefundedEvent;
import com.coderhan.lastmission.shared.order.OrderType;
import lombok.RequiredArgsConstructor;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * 결제 환불(PaymentRefundedEvent) 시, 예약 주문이면 CONFIRMED → REFUNDED로 전환하고
 * 티켓 재고를 되돌린다.
 *
 * <p>{@link PaymentConfirmedEventListener}와 같은 이유로 {@code @ApplicationModuleListener}를
 * 쓴다 — 재고 복원까지 걸린 상태 전환이라 이벤트를 조용히 잃어버리면 안 된다.</p>
 */
@Component
@RequiredArgsConstructor
class PaymentRefundedEventListener {

    private final ReservationService reservationService;

    @ApplicationModuleListener
    public void on(PaymentRefundedEvent event) {
        if (event.orderType() != OrderType.RESERVATION) {
            return;
        }
        reservationService.refundOrder(event.orderId());
    }
}
