package com.coderhan.lastmission.payment.application;

import com.coderhan.lastmission.event.EventEndedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** 행사 종료(EventEndedEvent) 시 정산을 자동 생성한다. */
@Component
@RequiredArgsConstructor
class SettlementEventListener {

    private final SettlementService settlementService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(EventEndedEvent event) {
        settlementService.create(event.eventId());
    }
}
