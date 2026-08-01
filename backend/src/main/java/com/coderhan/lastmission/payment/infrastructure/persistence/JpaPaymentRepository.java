package com.coderhan.lastmission.payment.infrastructure.persistence;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import com.coderhan.lastmission.payment.application.PaymentRepository;
import com.coderhan.lastmission.payment.domain.OrderType;
import com.coderhan.lastmission.payment.domain.Payment;
import com.coderhan.lastmission.payment.domain.PaymentStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class JpaPaymentRepository implements PaymentRepository {
    private final PaymentJpaRepository jpaRepository;

    @Override
    public Payment save(String orderId, OrderType orderType, Long userId,
                        String idempotencyKey, BigDecimal amount, String method,
                        String pgProvider, String pgOrderId, String pgTransactionId,
                        OffsetDateTime paidAt) {
        OffsetDateTime now = OffsetDateTime.now();

        PaymentEntity saved = jpaRepository.save(
                new PaymentEntity(
                    orderId, orderType, userId,
                    idempotencyKey, amount, method,
                    pgProvider, pgOrderId, pgTransactionId,
                    paidAt, now
                )
        );

        return toDomain(saved);
    }

    @Override
    public Optional<Payment> findById(long id) {
        return jpaRepository.findById(id).map(JpaPaymentRepository::toDomain);
    }

    @Override
    public Optional<Payment> findByIdempotencyKey(String idempotencyKey) {
        return jpaRepository.findByIdempotencyKey(idempotencyKey).map(JpaPaymentRepository::toDomain);
    }

    @Override
    public Optional<Payment> findByOrderIdAndStatus(String orderId, PaymentStatus status) {
        return jpaRepository.findByOrderIdAndStatus(orderId, status).map(JpaPaymentRepository::toDomain);
    }

    @Override
    public List<Payment> findByUserId(long userId) {
        return jpaRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(JpaPaymentRepository::toDomain)
                .toList();
    }

    private static Payment toDomain(PaymentEntity entity) {
        return Payment.builder()
                .id(entity.getId())
                .orderId(entity.getOrderId())
                .orderType(entity.getOrderType())
                .userId(entity.getUserId())
                .idempotencyKey(entity.getIdempotencyKey())
                .amount(entity.getAmount())
                .method(entity.getMethod())
                .status(entity.getStatus())
                .pgProvider(entity.getPgProvider())
                .pgOrderId(entity.getPgOrderId())
                .pgTransactionId(entity.getPgTransactionId())
                .paidAt(entity.getPaidAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
