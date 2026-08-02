package com.coderhan.lastmission.payment.application;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import com.coderhan.lastmission.payment.domain.Payment;
import com.coderhan.lastmission.payment.domain.Refund;
import com.coderhan.lastmission.shared.event.PaymentConfirmedEvent;
import com.coderhan.lastmission.shared.event.PaymentFailedEvent;
import com.coderhan.lastmission.shared.event.PaymentRefundedEvent;
import com.coderhan.lastmission.shared.order.OrderType;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

/**
 * 결제 승인/실패/환불 결과를 기록하고 {@link PaymentConfirmedEvent}/{@link PaymentFailedEvent}/
 * {@link PaymentRefundedEvent}를 발행하는 전담 협력 객체. 트랜잭션 커밋 이후에만 실행되는
 * @TransactionalEventListener(AFTER_COMMIT) 리스너가 정상적으로 등록되려면 publishEvent() 호출 시점에
 * 활성 트랜잭션이 있어야 하므로, "결제 결과를 기록/발행하는" 책임을 여기 한 군데로 모아
 * PaymentService/RefundService는 검증·오케스트레이션만 담당하게 한다.
 */
@Component
@RequiredArgsConstructor
class PaymentEventRecorder {

    private final PaymentRepository repository;
    private final PaymentLogRepository paymentLogRepository;
    private final RefundRepository refundRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

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

    /**
     * FAILED 감사 로그를 payment_logs에 저장 + {@link PaymentFailedEvent} 발행. PG 승인 자체가 없어
     * payments 테이블(idempotency_key/method 등 NOT NULL)에는 채울 실제 값이 없다 — payment_logs는
     * payment_key/request_payload가 전부 nullable이라 이런 감사 로그 용도에 원래 맞는 테이블이다.
     * 같은 주문에 실패 로그가 여러 건 쌓이는 것도 허용한다(webhookTransmissionId가 없어 중복 방지 대상이
     * 아님 — "결제가 안 됐다"는 사실 자체가 중요하지 재시도 중복 방지가 목적이 아니기 때문).
     */
    @Transactional
    void reportFailure(String orderId, OrderType orderType, Long userId, BigDecimal amount,
                       String reason, OffsetDateTime failedAt) {
        String requestPayload = objectMapper.writeValueAsString(
                new FailureLogPayload(orderId, orderType, userId, amount, reason));
        paymentLogRepository.save(null, "PAYMENT_FAILED", requestPayload, null, null, failedAt);

        eventPublisher.publishEvent(new PaymentFailedEvent(
            orderId,
            orderType,
            userId,
            reason,
            failedAt
        ));
    }

    private record FailureLogPayload(String orderId, OrderType orderType, Long userId, BigDecimal amount,
                                     String reason) {
    }

    /**
     * 환불 저장 + {@link PaymentRefundedEvent} 발행. RefundService.refund()는 private 메서드를 거치는
     * self-invocation이라(request() → refund()) 직접 @Transactional을 못 쓴다 — confirmAndSave()와
     * 동일한 이유로 이 협력 객체로 뺐다.
     */
    @Transactional
    Refund reportRefund(long paymentId, String orderId, OrderType orderType, BigDecimal amount, String reason,
                        OffsetDateTime refundedAt) {
        Refund refund = refundRepository.save(paymentId, amount, reason, refundedAt);

        eventPublisher.publishEvent(new PaymentRefundedEvent(
            orderId,
            orderType,
            paymentId,
            amount,
            refundedAt
        ));

        return refund;
    }
}
