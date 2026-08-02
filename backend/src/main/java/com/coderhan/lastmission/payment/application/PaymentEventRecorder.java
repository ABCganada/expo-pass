package com.coderhan.lastmission.payment.application;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import com.coderhan.lastmission.payment.domain.Payment;
import com.coderhan.lastmission.shared.event.PaymentConfirmedEvent;
import com.coderhan.lastmission.shared.event.PaymentFailedEvent;
import com.coderhan.lastmission.shared.order.OrderType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 결제 승인/실패 결과를 기록하고 {@link PaymentConfirmedEvent}/{@link PaymentFailedEvent}를
 * 발행하는 전담 협력 객체. 트랜잭션 커밋 이후에만 실행되는 @TransactionalEventListener(AFTER_COMMIT)
 * 리스너가 정상적으로 등록되려면 publishEvent() 호출 시점에 활성 트랜잭션이 있어야 하므로,
 * "결제 결과를 기록/발행하는" 책임을 여기 한 군데로 모아 PaymentService는 검증·오케스트레이션만
 * 담당하게 한다.
 */
@Component
@RequiredArgsConstructor
class PaymentEventRecorder {

    private final PaymentRepository repository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    Payment reportConfirmation(String orderId, OrderType orderType, Long userId,
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

    @Transactional
    void reportFailure(String orderId, OrderType orderType, Long userId,
                       String reason, OffsetDateTime failedAt) {
        eventPublisher.publishEvent(new PaymentFailedEvent(
            orderId,
            orderType,
            userId,
            reason,
            failedAt
        ));
    }
}
