package com.coderhan.lastmission.payment.infrastructure.persistence;

import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

interface PaymentLogJpaRepository extends JpaRepository<PaymentLogEntity, Long> {
    Optional<PaymentLogEntity> findByWebhookTransmissionId(String webhookTransmissionId);
    List<PaymentLogEntity> findAllByOrderByCreatedAtDesc();
}
