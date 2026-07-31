package com.coderhan.lastmission.payment.application;

import java.time.OffsetDateTime;
import java.util.List;
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

    /** 전체 로그 목록. 최신순 */
    List<PaymentLog> findAll();

    Optional<PaymentLog> findById(long id);
}
