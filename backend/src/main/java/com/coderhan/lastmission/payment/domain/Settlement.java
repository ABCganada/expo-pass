package com.coderhan.lastmission.payment.domain;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import lombok.Builder;

@Builder
public record Settlement(
        long id,
        long eventId,
        BigDecimal totalSales,
        BigDecimal commissionRate,
        BigDecimal commissionAmount,
        BigDecimal netAmount,
        SettlementStatus status,
        OffsetDateTime settledAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
