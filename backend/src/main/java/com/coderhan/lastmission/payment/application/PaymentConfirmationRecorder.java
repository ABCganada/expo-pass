package com.coderhan.lastmission.payment.application;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import com.coderhan.lastmission.payment.domain.Payment;
import com.coderhan.lastmission.shared.event.PaymentConfirmedEvent;
import com.coderhan.lastmission.shared.order.OrderType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 결제 저장과 {@link PaymentConfirmedEvent} 발행을 하나의 트랜잭션으로 묶는 전담 협력 객체.
 * PaymentService에서 직접 @Transactional을 쓸 수 없어서(private 메서드 self-invocation이라
 * 프록시를 안 거침) 별도 빈으로 분리했다 — EventEndedEventPublisher와 동일한 패턴.
 * 트랜잭션 커밋 이후에만 실행되는 @TransactionalEventListener(AFTER_COMMIT) 리스너가
 * 정상적으로 등록되려면, publishEvent() 호출 시점에 활성 트랜잭션이 있어야 한다.
 */
@Component
@RequiredArgsConstructor
class PaymentConfirmationRecorder {

    private final PaymentRepository repository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    Payment save(String orderId, OrderType orderType, Long userId,
                 String idempotencyKey, BigDecimal amount, String method,
                 String pgProvider, String pgOrderId, String pgTransactionId,
                 OffsetDateTime paidAt) {
        Payment payment = repository.save(
            orderId, orderType, userId,
            idempotencyKey, amount, method,
            pgProvider, pgOrderId, pgTransactionId,
            paidAt
        );

        eventPublisher.publishEvent(new PaymentConfirmedEvent(
            payment.id(),
            payment.orderId(),
            payment.orderType(),
            payment.paidAt()
        ));

        return payment;
    }
}
