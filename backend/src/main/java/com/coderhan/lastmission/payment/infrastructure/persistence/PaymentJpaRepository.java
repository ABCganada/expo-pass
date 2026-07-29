package com.coderhan.lastmission.payment.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import com.coderhan.lastmission.payment.domain.PaymentStatus;
import org.springframework.data.jpa.repository.JpaRepository;

interface PaymentJpaRepository extends JpaRepository<PaymentEntity, Long> {
    Optional<PaymentEntity> findByIdempotencyKey(String idempotencyKey);
    Optional<PaymentEntity> findByOrderIdAndStatus(String orderId, PaymentStatus status);
    List<PaymentEntity> findByUserIdOrderByCreatedAtDesc(Long userId);
}
