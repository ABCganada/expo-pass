package com.coderhan.lastmission.payment.infrastructure.persistence;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import com.coderhan.lastmission.payment.domain.RefundStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@Table(name = "payment_refunds")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class RefundEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;  // DB 기본값 unique_rowid() — CockroachDB hot range 방지

    @Column(name = "payment_id", nullable = false)
    private Long paymentId;  // payments.id 참조 (FK 설정됨)

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;  // 부분 환불 미지원 — 항상 payments.amount와 동일

    @Column(name = "reason")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private RefundStatus status;

    @Column(name = "is_auto_approved", nullable = false)
    private boolean autoApproved;

    @Column(name = "approved_by")
    private Long approvedBy;  // user_accounts.id 참조, 논리적 참조. 관리자 승인 플로우에서 채워짐

    @Column(name = "requested_at", nullable = false)
    private OffsetDateTime requestedAt;

    @Column(name = "refunded_at")
    private OffsetDateTime refundedAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    /** 자동승인 대상. 토스 취소가 이미 성공한 뒤 호출되므로 바로 COMPLETED 상태로 바로 만든다. */
    static RefundEntity completed(Long paymentId, BigDecimal amount, String reason, OffsetDateTime completedAt) {
        RefundEntity entity = new RefundEntity();
        entity.paymentId = paymentId;
        entity.amount = amount;
        entity.reason = reason;
        entity.status = RefundStatus.COMPLETED;
        entity.autoApproved = true;
        entity.requestedAt = completedAt;
        entity.refundedAt = completedAt;
        entity.createdAt = completedAt;
        entity.updatedAt = completedAt;
        return entity;
    }

    /** 수동승인(이벤트 관리자) 대상. REQUESTED 상태로만 만든다 — 승인/거절은 별도 플로우 */
    static RefundEntity requestedByEventAdmin(Long paymentId, BigDecimal amount, String reason, OffsetDateTime now) {
        RefundEntity entity = new RefundEntity();
        entity.paymentId = paymentId;
        entity.amount = amount;
        entity.reason = reason;
        entity.status = RefundStatus.REQUESTED;
        entity.autoApproved = false;
        entity.requestedAt = now;
        entity.createdAt = now;
        entity.updatedAt = now;
        return entity;
    }
}
