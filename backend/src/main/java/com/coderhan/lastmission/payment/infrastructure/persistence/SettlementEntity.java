package com.coderhan.lastmission.payment.infrastructure.persistence;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import com.coderhan.lastmission.payment.domain.SettlementStatus;
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
@Table(name = "payment_settlements")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class SettlementEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;  // DB 기본값 unique_rowid() — CockroachDB hot range 방지

    @Column(name = "event_id", nullable = false)
    private Long eventId;  // Event 도메인 events.id 참조, 논리적 참조

    @Column(name = "total_sales", nullable = false)
    private BigDecimal totalSales;

    @Column(name = "commission_rate", nullable = false)
    private BigDecimal commissionRate;  // 퍼센트 값 그대로 저장 (예: 5.00 = 5%)

    @Column(name = "commission_amount", nullable = false)
    private BigDecimal commissionAmount;

    @Column(name = "net_amount", nullable = false)
    private BigDecimal netAmount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private SettlementStatus status;

    @Column(name = "settled_at", nullable = false)
    private OffsetDateTime settledAt;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    /** 행사 종료 감지 즉시 확정. 대기 상태 없이 바로 COMPLETED로 만든다. */
    static SettlementEntity completed(Long eventId, BigDecimal totalSales, BigDecimal commissionRate,
                                       BigDecimal commissionAmount, BigDecimal netAmount, OffsetDateTime settledAt) {
        SettlementEntity entity = new SettlementEntity();
        entity.eventId = eventId;
        entity.totalSales = totalSales;
        entity.commissionRate = commissionRate;
        entity.commissionAmount = commissionAmount;
        entity.netAmount = netAmount;
        entity.status = SettlementStatus.COMPLETED;
        entity.settledAt = settledAt;
        entity.createdAt = settledAt;
        entity.updatedAt = settledAt;
        return entity;
    }
}
