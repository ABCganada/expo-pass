package com.coderhan.lastmission.payment.application;

import java.math.BigDecimal;
import java.util.Optional;
import com.coderhan.lastmission.payment.domain.Refund;

public interface RefundRepository {

    /** 신청 접수 시점엔 항상 REQUESTED 상태로 저장한다 — 승인/거절/완료는 별도 플로우에서 처리. */
    Refund save(long paymentId, BigDecimal amount, String reason);

    /** 결제 1건당 진행 중인(REQUESTED/APPROVED/COMPLETED) 환불이 있는지 조회 — 중복 신청 방지용 */
    Optional<Refund> findActiveByPaymentId(long paymentId);
}
