package com.coderhan.lastmission.payment.infrastructure.persistence;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import com.coderhan.lastmission.shared.order.OrderType;
import com.coderhan.lastmission.payment.domain.PaymentStatus;
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
@Table(name = "payments")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class PaymentEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;  // DB 기본값 unique_rowid() — CockroachDB hot range 방지

    @Column(name = "order_id", nullable = false)
    private String orderId;  // reservation_orders.order_id 또는 marketing_banner_ads.order_id 논리적 참조

    @Enumerated(EnumType.STRING)
    @Column(name = "order_type", nullable = false)
    private OrderType orderType;

    @Column(name = "user_id")
    private Long userId;  // user_accounts.id 참조, 논리적 참조 (내 결제 내역 조회용)

    @Column(name = "idempotency_key", nullable = false)
    private String idempotencyKey;  // 결제 요청 재시도 시 중복 처리 방지용, 유니크 인덱스 걸림

    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Column(name = "method", nullable = false)
    private String method;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private PaymentStatus status;

    @Column(name = "pg_provider", nullable = false)
    private String pgProvider;

    @Column(name = "pg_order_id")
    private String pgOrderId;

    @Column(name = "pg_transaction_id")
    private String pgTransactionId;

    @Column(name = "paid_at")
    private OffsetDateTime paidAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    /** 승인(confirm) 성공 직후 COMPLETED 상태로 바로 만든다 — REQUESTED row를 거치지 않는다. */
    PaymentEntity(
        String orderId, OrderType orderType, Long userId,
        String idempotencyKey, BigDecimal amount, String method,
        String pgProvider, String pgOrderId, String pgTransactionId,
        OffsetDateTime paidAt, OffsetDateTime now
    ) {
        this.orderId = orderId;
        this.orderType = orderType;
        this.userId = userId;
        this.idempotencyKey = idempotencyKey;
        this.amount = amount;
        this.method = method;
        this.status = PaymentStatus.COMPLETED;
        this.pgProvider = pgProvider;
        this.pgOrderId = pgOrderId;
        this.pgTransactionId = pgTransactionId;
        this.paidAt = paidAt;
        this.createdAt = now;
        this.updatedAt = now;
    }
}
