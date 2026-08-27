package com.coderhan.lastmission.payment.application;

import com.coderhan.lastmission.marketing.AdRejectedEvent;
import com.coderhan.lastmission.payment.domain.Payment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.modulith.events.ApplicationModuleListener;
import org.springframework.stereotype.Component;

/**
 * 광고 반려(AdRejectedEvent) 시, 해당 결제를 자동으로 전액 환불한다(광고 환불 정책 — 항상 100%).
 * 관리자가 남의 결제를 대신 환불 신청하는 셈이라, RefundService.request()의 소유자 검증을 통과하도록
 * 결제 소유자(payment.userId())를 그대로 넘겨 호출한다.
 *
 * <p>{@code @ApplicationModuleListener}를 쓴다 — 환불까지 걸린 처리라 이벤트를 조용히 잃어버리면 안 된다.</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
class AdRejectedEventListener {

    private final PaymentRepository paymentRepository;
    private final RefundService refundService;

    @ApplicationModuleListener
    public void on(AdRejectedEvent event) {
        Payment payment = paymentRepository.findByOrderId(event.orderId()).orElse(null);
        if (payment == null) {
            log.warn("반려된 광고의 결제를 찾을 수 없음. adId={}, orderId={}", event.adId(), event.orderId());
            return;
        }
        refundService.request(payment.userId(), payment.id(), "관리자에 의해 광고가 반려되어 자동 환불되었습니다.");
    }
}
