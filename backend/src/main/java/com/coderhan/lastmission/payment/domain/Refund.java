package com.coderhan.lastmission.payment.domain;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Builder;

@Builder
public record Refund(
        long id,
        long paymentId,
        BigDecimal amount,
        String reason,
        RefundStatus status,
        boolean autoApproved,
        Long approvedBy,
        OffsetDateTime requestedAt,
        OffsetDateTime refundedAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
