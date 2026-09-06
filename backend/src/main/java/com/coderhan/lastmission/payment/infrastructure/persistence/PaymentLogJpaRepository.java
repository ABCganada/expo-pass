package com.coderhan.lastmission.payment.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface PaymentLogJpaRepository extends JpaRepository<PaymentLogEntity, Long> {
    Optional<PaymentLogEntity> findByWebhookTransmissionId(String webhookTransmissionId);
    Page<PaymentLogEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @Query(value = "SELECT unique_rowid()", nativeQuery = true)
    long nextId();
    
    /** order_id 컬럼이 없어 request_payload의 orderId로 매칭. status=DONE인 것만 승인 완료로 본다. */
    @Query(value = """
            SELECT payment_key AS paymentKey,
                   request_payload->>'amount' AS amount,
                   response_payload->>'method' AS method,
                   response_payload->>'approvedAt' AS approvedAt
            FROM payment_logs
            WHERE action = 'APPROVE'
              AND request_payload->>'orderId' = :orderId
              AND response_payload->>'status' = 'DONE'
            ORDER BY created_at DESC
            LIMIT 1
            """, nativeQuery = true)
    Optional<ApprovedPaymentLogRow> findApprovedByOrderId(@Param("orderId") String orderId);

    interface ApprovedPaymentLogRow {
        String getPaymentKey();
        String getAmount();
        String getMethod();
        String getApprovedAt();
    }
}
