package com.coderhan.lastmission.payment.domain;

import java.math.BigDecimal;

public record AdSettlementSummary(
        BigDecimal totalAmount,
        BigDecimal totalNetAmount,
        long settlementCount
) {
}
