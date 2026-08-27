package com.coderhan.lastmission.payment.application;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

/**
 * PG에 결제 승인을 요청하는 포트.
 * 구현체는 infrastructure 계층에 둔다(예: infrastructure.toss.TossPaymentGateway)
 */
public interface PaymentGateway {

    ConfirmResult confirm(String paymentKey, String pgOrderId, BigDecimal amount);
    CancelResult cancel(String paymentKey, String reason, BigDecimal amount);

    record ConfirmResult(String method, OffsetDateTime approvedAt) {}

    record CancelResult(OffsetDateTime canceledAt) {}
}
