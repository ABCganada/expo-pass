package com.coderhan.lastmission.payment.domain;

import java.math.BigDecimal;

public record SettlementSummary(
        BigDecimal totalSales,
        BigDecimal totalCommissionAmount,
        long settlementCount
) {
}
