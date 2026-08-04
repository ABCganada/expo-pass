package com.coderhan.lastmission.payment.domain;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import lombok.Builder;

@Builder
public record AdSettlement(
        long id,
        UUID adId,
        BigDecimal totalAmount,
        BigDecimal commissionRate,
        BigDecimal commissionAmount,
        BigDecimal netAmount,
        SettlementStatus status,
        OffsetDateTime settledAt,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
