package com.coderhan.lastmission.payment.infrastructure.persistence;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import com.coderhan.lastmission.payment.application.RefundRepository;
import com.coderhan.lastmission.payment.domain.Refund;
import com.coderhan.lastmission.payment.domain.RefundStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
class JpaRefundRepository implements RefundRepository {

    /** REQUESTED, APPROVED, COMPLETED 에 대한 새 환불 요청 막아야 함 */
    private static final List<RefundStatus> ACTIVE_STATUSES =
            List.of(RefundStatus.REQUESTED, RefundStatus.APPROVED, RefundStatus.COMPLETED);

    private final RefundJpaRepository jpaRepository;

    @Override
    public Refund save(long paymentId, BigDecimal amount, String reason, OffsetDateTime completedAt) {
        RefundEntity saved = jpaRepository.save(RefundEntity.completed(paymentId, amount, reason, completedAt));

        return toDomain(saved);
    }

    @Override
    public Refund saveAsRequested(long paymentId, BigDecimal amount, String reason) {
        OffsetDateTime now = OffsetDateTime.now();

        RefundEntity saved = jpaRepository.save(RefundEntity.requestedByEventAdmin(paymentId, amount, reason, now));

        return toDomain(saved);
    }

    @Override
    public Optional<Refund> findActiveByPaymentId(long paymentId) {
        return jpaRepository.findFirstByPaymentIdAndStatusIn(paymentId, ACTIVE_STATUSES)
                .map(JpaRefundRepository::toDomain);
    }

    @Override
    public Optional<Refund> findById(long refundId) {
        return jpaRepository.findById(refundId).map(JpaRefundRepository::toDomain);
    }

    @Override
    public Refund approve(long refundId, long approvedBy, OffsetDateTime completedAt) {
        RefundEntity entity = jpaRepository.findById(refundId).orElseThrow();
        entity.approve(approvedBy, completedAt);

        return toDomain(jpaRepository.save(entity));
    }

    @Override
    public Refund reject(long refundId, long decidedBy, OffsetDateTime decidedAt) {
        RefundEntity entity = jpaRepository.findById(refundId).orElseThrow();
        entity.reject(decidedBy, decidedAt);

        return toDomain(jpaRepository.save(entity));
    }

    private static Refund toDomain(RefundEntity entity) {
        return Refund.builder()
                .id(entity.getId())
                .paymentId(entity.getPaymentId())
                .amount(entity.getAmount())
                .reason(entity.getReason())
                .status(entity.getStatus())
                .autoApproved(entity.isAutoApproved())
                .approvedBy(entity.getApprovedBy())
                .requestedAt(entity.getRequestedAt())
                .refundedAt(entity.getRefundedAt())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
