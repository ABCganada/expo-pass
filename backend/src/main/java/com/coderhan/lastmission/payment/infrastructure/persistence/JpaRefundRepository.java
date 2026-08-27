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

    private static final List<RefundStatus> ACTIVE_STATUSES = List.of(RefundStatus.COMPLETED);

    private final RefundJpaRepository jpaRepository;

    @Override
    public Refund save(long paymentId, BigDecimal amount, String reason, OffsetDateTime completedAt) {
        jpaRepository.save(RefundEntity.completed(paymentId, amount, reason, completedAt));

        RefundEntity saved = jpaRepository.findFirstByPaymentIdAndStatusIn(paymentId, ACTIVE_STATUSES)
                .orElseThrow(() -> new IllegalStateException("환불 저장 직후 조회에 실패했습니다. paymentId=" + paymentId));

        return toDomain(saved);
    }

    @Override
    public Optional<Refund> findActiveByPaymentId(long paymentId) {
        return jpaRepository.findFirstByPaymentIdAndStatusIn(paymentId, ACTIVE_STATUSES)
                .map(JpaRefundRepository::toDomain);
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
