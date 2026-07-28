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
    public static void validate(String orderId, String idempotencyKey, BigDecimal amount, String method,
            String pgProvider) {
        if (orderId == null || orderId.isBlank()) {
            throw new BusinessException(ErrorCode.PAYMENT_INVALID_REQUEST, "주문 ID가 올바르지 않습니다.");
        }
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new BusinessException(ErrorCode.PAYMENT_INVALID_REQUEST, "idempotency key가 필요합니다.");
        }
        if (amount == null || amount.signum() <= 0) {
            throw new BusinessException(ErrorCode.PAYMENT_INVALID_REQUEST, "결제 금액이 올바르지 않습니다.");
        }
        if (method == null || method.isBlank()) {
            throw new BusinessException(ErrorCode.PAYMENT_INVALID_REQUEST, "결제 수단이 올바르지 않습니다.");
        }
        if (pgProvider == null || pgProvider.isBlank()) {
            throw new BusinessException(ErrorCode.PAYMENT_INVALID_REQUEST, "PG사가 올바르지 않습니다.");
        }
    }
}
