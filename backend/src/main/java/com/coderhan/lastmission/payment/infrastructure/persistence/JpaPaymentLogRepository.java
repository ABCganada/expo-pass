package com.coderhan.lastmission.payment.infrastructure.persistence;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import com.coderhan.lastmission.payment.application.PaymentLogRepository;
import com.coderhan.lastmission.payment.domain.PaymentLog;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class JpaPaymentLogRepository implements PaymentLogRepository {

    private final PaymentLogJpaRepository jpaRepository;

    @Override
    public PaymentLog save(String paymentKey, String action, String requestPayload, String responsePayload,
                           String webhookTransmissionId, OffsetDateTime createdAt) {
        try {
            PaymentLogEntity saved = jpaRepository.save(
                    PaymentLogEntity.create(paymentKey, action, requestPayload, responsePayload,
                            webhookTransmissionId, createdAt));

            return toDomain(saved);
        } catch (DataIntegrityViolationException e) {
            if (webhookTransmissionId == null) {
                throw e;
            }

            return jpaRepository.findByWebhookTransmissionId(webhookTransmissionId)
                    .map(JpaPaymentLogRepository::toDomain)
                    .orElseThrow(() -> e);
        }
    }

    @Override
    public List<PaymentLog> findAll() {
        return jpaRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(JpaPaymentLogRepository::toDomain)
                .toList();
    }

    @Override
    public Optional<PaymentLog> findById(long id) {
        return jpaRepository.findById(id)
            .map(JpaPaymentLogRepository::toDomain);
    }

    private static PaymentLog toDomain(PaymentLogEntity entity) {
        return PaymentLog.builder()
                .id(entity.getId())
                .paymentKey(entity.getPaymentKey())
                .action(entity.getAction())
                .requestPayload(entity.getRequestPayload())
                .responsePayload(entity.getResponsePayload())
                .webhookTransmissionId(entity.getWebhookTransmissionId())
                .createdAt(entity.getCreatedAt())
                .build();
    }
}
