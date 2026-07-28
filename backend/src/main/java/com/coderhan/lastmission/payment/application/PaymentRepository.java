package com.coderhan.lastmission.payment.application;

import java.math.BigDecimal;
import java.util.Optional;
import com.coderhan.lastmission.payment.domain.Payment;
import com.coderhan.lastmission.payment.domain.PaymentStatus;

public interface PaymentRepository {
    Payment save(String orderId, String idempotencyKey, BigDecimal amount, String method, String pgProvider);
    Optional<Payment> findByIdempotencyKey(String idempotencyKey);
    Optional<Payment> findByOrderIdAndStatus(String orderId, PaymentStatus status);  // 이중 결제 여부 체크용
}
