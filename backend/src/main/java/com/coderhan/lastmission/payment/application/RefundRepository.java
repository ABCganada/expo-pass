package com.coderhan.lastmission.payment.application;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import com.coderhan.lastmission.payment.domain.Refund;

public interface RefundRepository {

    /** 자동승인 신청 접수 (0원 환불 포함, 항상 COMPLETED로 저장) */
    Refund save(long paymentId, BigDecimal amount, String reason, OffsetDateTime completedAt);

    /** 결제 1건당 진행 중인(COMPLETED) 환불이 있는지 조회 — 중복 신청 방지용 */
    Optional<Refund> findActiveByPaymentId(long paymentId);
}
