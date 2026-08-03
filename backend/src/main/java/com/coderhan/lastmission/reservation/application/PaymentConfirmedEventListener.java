package com.coderhan.lastmission.reservation.application;

import com.coderhan.lastmission.shared.event.PaymentConfirmedEvent;
import com.coderhan.lastmission.shared.order.OrderType;
import lombok.RequiredArgsConstructor;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * 결제 승인(PaymentConfirmedEvent) 시, 예약 주문이면 PENDING → CONFIRMED로 전환한다.
 *
 * <p>일반 {@code @TransactionalEventListener} 대신 {@code @ApplicationModuleListener}를 쓴다 —
 * 이벤트 발행 시점에 event_publication 테이블에 미완료 기록을 남기고, 리스너가 예외 없이 끝나야
 * completion_date를 채운다. 처리 중 실패하거나 서버가 죽어도 재시작 시 자동 재시도된다
 * (application.yaml의 republish-outstanding-events-on-restart=true). 돈이 걸린 상태 전환이라
 * 이벤트를 조용히 잃어버리면 안 되므로, 이 durability가 반드시 필요하다.</p>
 */
@Component
@RequiredArgsConstructor
class PaymentConfirmedEventListener {

    private final ReservationService reservationService;

    @ApplicationModuleListener
    public void on(PaymentConfirmedEvent event) {
        if (event.orderType() != OrderType.RESERVATION) {
            return;
        }
        reservationService.confirmOrder(event.orderId());
    }
}
