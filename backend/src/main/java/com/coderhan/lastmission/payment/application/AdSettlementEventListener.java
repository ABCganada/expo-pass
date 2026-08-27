package com.coderhan.lastmission.payment.application;

import com.coderhan.lastmission.marketing.AdExpiredEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** 광고 만료(AdExpiredEvent) 시 광고 정산을 자동 생성한다. */
@Component
@RequiredArgsConstructor
class AdSettlementEventListener {

    private final AdSettlementService adSettlementService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(AdExpiredEvent event) {
        adSettlementService.create(event.adId(), event.totalAmount());
    }
}
