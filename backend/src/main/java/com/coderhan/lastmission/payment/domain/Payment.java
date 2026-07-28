package com.coderhan.lastmission.payment.domain;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import com.coderhan.lastmission.shared.error.BusinessException;
import com.coderhan.lastmission.shared.error.ErrorCode;
import lombok.Builder;

@Builder
public record Payment(
        long id,
        String orderId,
        String idempotencyKey,
        BigDecimal amount,
        String method,
        PaymentStatus status,
        String pgProvider,
        String pgOrderId,
        String pgTransactionId,
        OffsetDateTime paidAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static void validate(String orderId, String pgOrderId, String paymentKey, BigDecimal amount) {
        if (orderId == null || orderId.isBlank()) {
            throw new BusinessException(ErrorCode.PAYMENT_INVALID_REQUEST, "주문 ID가 올바르지 않습니다.");
        }
        if (pgOrderId == null || pgOrderId.isBlank()) {
            throw new BusinessException(ErrorCode.PAYMENT_INVALID_REQUEST, "PG 주문 ID가 올바르지 않습니다.");
        }
        if (paymentKey == null || paymentKey.isBlank()) {
            throw new BusinessException(ErrorCode.PAYMENT_INVALID_REQUEST, "paymentKey가 올바르지 않습니다.");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new BusinessException(ErrorCode.PAYMENT_INVALID_REQUEST, "결제 금액이 올바르지 않습니다.");
        }
    }
}
