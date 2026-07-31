package com.coderhan.lastmission.payment.domain;

import java.math.BigDecimal;

public record SettlementSummary(
        BigDecimal totalSales,
        BigDecimal totalCommissionAmount,
        BigDecimal totalNetAmount,
        long settlementCount
) {
}
