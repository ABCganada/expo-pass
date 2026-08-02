package com.coderhan.lastmission.payment.application;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import com.coderhan.lastmission.shared.order.OrderType;
import com.coderhan.lastmission.payment.domain.Payment;
import com.coderhan.lastmission.payment.domain.PaymentStatus;

public interface PaymentRepository {

    /** 토스 confirm 성공 직후, COMPLETED 상태로 바로 저장한다(승인 전 REQUESTED row를 따로 안 만듦). */
    Payment save(String orderId, OrderType orderType, Long userId,
                 String idempotencyKey, BigDecimal amount, String method,
                 String pgProvider, String pgOrderId, String pgTransactionId,
                 OffsetDateTime paidAt);

    Optional<Payment> findById(long id);
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);
    Optional<Payment> findByOrderIdAndStatus(String orderId, PaymentStatus status);  // 이중 결제 여부 체크용
    List<Payment> findByUserId(long userId);  // 내 결제 내역 조회용, 최신순

    /** 환불 확정 직후 payments.status를 REFUNDED로 갱신한다. */
    void markRefunded(long paymentId, OffsetDateTime refundedAt);
}
