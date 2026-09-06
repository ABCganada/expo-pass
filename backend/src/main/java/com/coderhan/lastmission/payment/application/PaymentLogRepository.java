package com.coderhan.lastmission.payment.application;

import java.math.BigDecimal;
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
     * orderId로 실제 승인 완료된(status=DONE) APPROVE 기록을 찾는다. action="APPROVE"는 승인 실패
     * 시에도 남으므로 action만으로는 판단할 수 없다. payments 저장 유실 주문 재확인용.
     */
    Optional<ApprovedPaymentLog> findApprovedByOrderId(String orderId);

    record ApprovedPaymentLog(String paymentKey, BigDecimal amount, String method, OffsetDateTime approvedAt) {}
}
