package com.coderhan.lastmission.marketing.application;

import com.coderhan.lastmission.shared.event.PaymentConfirmedEvent;
import com.coderhan.lastmission.shared.event.PaymentRefundedEvent;
import com.coderhan.lastmission.shared.order.OrderType;
import lombok.RequiredArgsConstructor;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
class PaymentEventListener {

    private final BannerAdService bannerAdService;

    @ApplicationModuleListener
    public void on(PaymentConfirmedEvent event) {
        if (event.orderType() != OrderType.ADVERTISEMENT) {
            return;
        }
        bannerAdService.approveByOrderId(event.orderId());
    }

    @ApplicationModuleListener
    public void on(PaymentRefundedEvent event) {
        if (event.orderType() != OrderType.ADVERTISEMENT) {
            return;
        }
        bannerAdService.refundByOrderId(event.orderId());
    }
}
