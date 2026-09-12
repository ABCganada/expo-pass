package com.coderhan.lastmission.event.application;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/** 행사 상세 조회 트랜잭션이 커밋된 뒤 조회수 증가 */
@Component
@RequiredArgsConstructor
class EventViewedEventListener {

    private final EventViewService eventViewService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(EventViewedEvent event) {
        eventViewService.increaseIfNew(event.eventId(), event.userId());
    }
}