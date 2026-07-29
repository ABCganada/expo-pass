package com.coderhan.lastmission.payment.application;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import com.coderhan.lastmission.payment.domain.Refund;

public interface RefundRepository {

    /** 자동승인 대상 신청 접수 */
    Refund save(long paymentId, BigDecimal amount, String reason, OffsetDateTime completedAt);

    /** 수동승인(이벤트 관리자) 대상 신청 접수. 항상 REQUESTED 상태로 저장한다 — 승인/거절은 별도 플로우에서 처리. */
    Refund saveAsRequested(long paymentId, BigDecimal amount, String reason);

    /** 결제 1건당 진행 중인(REQUESTED/APPROVED/COMPLETED) 환불이 있는지 조회 — 중복 신청 방지용 */
    Optional<Refund> findActiveByPaymentId(long paymentId);

    Optional<Refund> findById(long refundId);

    /** 이벤트 관리자 승인. REQUESTED -> COMPLETED */
    Refund approve(long refundId, long approvedBy, OffsetDateTime completedAt);

    /** 이벤트 관리자 거절. REQUESTED -> REJECTED */
    Refund reject(long refundId, long decidedBy, OffsetDateTime decidedAt);

    /** 이벤트 관리자가 승인/거절해야 할 대기 목록. 오래 기다린 순(신청일 오름차순) */
    List<Refund> findAllRequested();
}
