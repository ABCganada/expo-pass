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
    
    @Query(value = """
            SELECT EXISTS (
                SELECT 1 FROM payment_logs
                WHERE action = 'APPROVE'
                  AND request_payload->>'orderId' = :orderId
                  AND response_payload->>'status' = 'DONE'
            )
            """, nativeQuery = true)
    boolean existsApprovedOrderId(@Param("orderId") String orderId);
}
