package com.coderhan.lastmission.payment.domain;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
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
}
