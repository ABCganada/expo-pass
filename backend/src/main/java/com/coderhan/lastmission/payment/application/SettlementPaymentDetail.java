package com.coderhan.lastmission.payment.application;

import java.math.BigDecimal;
import com.coderhan.lastmission.payment.domain.Payment;

/** 정산 상세의 결제 내역 한 건(감사용) — 완료 결제 + 그 결제에 걸린 활성 환불액. */
public record SettlementPaymentDetail(Payment payment, BigDecimal refundedAmount) {
}
