package com.coderhan.lastmission.marketing.application;

import com.coderhan.lastmission.shared.event.PaymentRefundedEvent;
import com.coderhan.lastmission.shared.order.OrderType;
import lombok.RequiredArgsConstructor;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * 결제 환불(PaymentRefundedEvent) 시, 광고 주문이면 결제완료/승인 상태를 취소 처리한다.
 *
 * <p>{@link PaymentEventListener}와 같은 이유로 {@code @ApplicationModuleListener}를 쓴다 —
 * 노출 중단·정산 제외까지 걸린 상태 전환이라 이벤트를 조용히 잃어버리면 안 된다.</p>
 */
@Component
@RequiredArgsConstructor
class PaymentRefundedEventListener {

    private final BannerAdService bannerAdService;

    @ApplicationModuleListener
    public void on(PaymentRefundedEvent event) {
        if (event.orderType() != OrderType.ADVERTISEMENT) {
            return;
        }
        bannerAdService.cancelForRefund(event.orderId());
    }
}
