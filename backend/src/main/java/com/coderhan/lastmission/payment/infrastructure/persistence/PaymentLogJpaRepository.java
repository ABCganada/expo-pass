package com.coderhan.lastmission.payment.infrastructure.persistence;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

interface PaymentLogJpaRepository extends JpaRepository<PaymentLogEntity, Long> {
    Optional<PaymentLogEntity> findByWebhookTransmissionId(String webhookTransmissionId);
    Page<PaymentLogEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
