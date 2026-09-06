package com.coderhan.lastmission.payment.infrastructure.persistence;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import com.coderhan.lastmission.payment.application.PaymentLogPage;
import com.coderhan.lastmission.payment.application.PaymentLogRepository;
import com.coderhan.lastmission.payment.domain.PaymentLog;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class JpaPaymentLogRepository implements PaymentLogRepository {

    private final PaymentLogJpaRepository jpaRepository;

    @Override
    public PaymentLog save(String paymentKey, String action, String requestPayload, String responsePayload,
                           String webhookTransmissionId, OffsetDateTime createdAt) {
        try {
            long id = jpaRepository.nextId();
            PaymentLogEntity saved = jpaRepository.save(
                    PaymentLogEntity.create(id, paymentKey, action, requestPayload, responsePayload,
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
    public PaymentLogPage findAll(int page, int size) {
        int safeSize = Math.clamp(size, 1, 100);
        int safePage = Math.max(page, 0);

        Page<PaymentLogEntity> result = jpaRepository.findAllByOrderByCreatedAtDesc(
                PageRequest.of(safePage, safeSize));

        return new PaymentLogPage(
                result.stream().map(JpaPaymentLogRepository::toDomain).toList(),
                safePage, safeSize, result.getTotalElements());
    }

    @Override
    public Optional<PaymentLog> findById(long id) {
        return jpaRepository.findById(id)
            .map(JpaPaymentLogRepository::toDomain);
    }

    @Override
    public Optional<ApprovedPaymentLog> findApprovedByOrderId(String orderId) {
        return jpaRepository.findApprovedByOrderId(orderId)
                .map(row -> new ApprovedPaymentLog(
                        row.getPaymentKey(),
                        new BigDecimal(row.getAmount()),
                        row.getMethod(),
                        OffsetDateTime.parse(row.getApprovedAt())));
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
