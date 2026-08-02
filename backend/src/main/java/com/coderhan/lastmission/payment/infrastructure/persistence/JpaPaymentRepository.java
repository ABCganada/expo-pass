package com.coderhan.lastmission.payment.infrastructure.persistence;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import com.coderhan.lastmission.payment.application.PaymentRepository;
import com.coderhan.lastmission.shared.order.OrderType;
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
    public Optional<Payment> findByOrderId(String orderId) {
        return jpaRepository.findByOrderId(orderId).map(JpaPaymentRepository::toDomain);
    }

    @Override
    public List<Payment> findByUserId(long userId) {
        return jpaRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(JpaPaymentRepository::toDomain)
                .toList();
    }

    /** 호출부(PaymentEventRecorder)가 이미 연 트랜잭션 안에서 실행되므로, 여기서 조회한 영속 엔티티를
     * 변경해두면 별도 save() 없이 커밋 시점에 반영된다(JPA dirty checking). */
    @Override
    public void markRefunded(long paymentId, OffsetDateTime refundedAt) {
        PaymentEntity entity = jpaRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalStateException("결제 내역을 찾을 수 없습니다. paymentId=" + paymentId));
        entity.markRefunded(refundedAt);
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
