package com.coderhan.lastmission.payment.application;

import java.time.OffsetDateTime;
import java.util.Optional;
import com.coderhan.lastmission.payment.domain.PaymentLog;

public interface PaymentLogRepository {

    /**
     * 로그 저장. webhookTransmissionId가 이미 존재하면(웹훅 재시도 중복) 새로 만들지 않고
     * 기존 로그를 그대로 반환한다. 우리 쪽 API 호출 로그처럼 webhookTransmissionId가
     * null인 경우엔 이 중복 방지가 적용되지 않는다.
     */
    PaymentLog save(String paymentKey, String action, String requestPayload, String responsePayload,
                    String webhookTransmissionId, OffsetDateTime createdAt);

    /** 로그 목록 페이지 조회. 최신순 */
    PaymentLogPage findAll(int page, int size);

    Optional<PaymentLog> findById(long id);

    /**
     * 해당 주문(orderId)에 대해 실제로 승인이 완료된 토스 APPROVE 기록이 있는지 확인한다.
     *
     * TossPaymentGateway.confirm()은 승인 성공/실패 모두 action="APPROVE"로 감사 로그를 남기므로
     * action만으로는 판단할 수 없다 — 응답이 실제 승인 완료(status=DONE)인 경우만 true를 반환한다.
     * payments 테이블 저장이 실패해 결제 기록이 유실된 주문을 재확인하는 용도로 쓰인다.
     */
    boolean existsApprovedOrderId(String orderId);
}
