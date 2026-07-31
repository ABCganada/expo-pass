package com.coderhan.lastmission.payment.application;

import com.coderhan.lastmission.shared.event.EventEndedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 행사 종료(EventEndedEvent) 시 정산을 자동 생성한다.
 *
 * DRAFT — {@link EventEndedEvent}가 아직 Event 도메인에서 실제로 발행되지 않으므로,
 * 이 리스너는 지금 당장 호출되지 않는다. Event 도메인 담당자가 종료 감지 스케줄러를
 * 구현해 이 이벤트를 발행하기 시작하면 그때부터 동작한다.
 */
@Component
@RequiredArgsConstructor
class SettlementEventListener {

    private final SettlementService settlementService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(EventEndedEvent event) {
        settlementService.create(event.eventId());
    }
}
