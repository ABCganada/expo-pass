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

    /** 신청 접수 시점엔 REQUESTED 상태로만 만든다 — 자동승인/토스 취소는 별도 플로우가 담당한다. */
    RefundEntity(
            Long paymentId,
            BigDecimal amount,
            String reason,
            OffsetDateTime now
    ) {
        this.paymentId = paymentId;
        this.amount = amount;
        this.reason = reason;
        this.status = RefundStatus.REQUESTED;
        this.autoApproved = false;
        this.requestedAt = now;
        this.createdAt = now;
        this.updatedAt = now;
    }
}
