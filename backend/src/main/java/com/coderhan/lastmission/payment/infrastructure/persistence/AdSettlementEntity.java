package com.coderhan.lastmission.payment.infrastructure.persistence;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
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
@Table(name = "payment_ad_settlements")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
class AdSettlementEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;  // DB 기본값 unique_rowid() — CockroachDB hot range 방지

    @Column(name = "ad_id", nullable = false)
    private UUID adId;  // Marketing 도메인 marketing_banner_ads.id 참조, 논리적 참조

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount;

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

    /** 광고 만료 감지 즉시 확정. 대기 상태 없이 바로 COMPLETED로 만든다. */
    static AdSettlementEntity completed(
            UUID adId,
            BigDecimal totalAmount,
            BigDecimal netAmount,
            OffsetDateTime settledAt
    ) {
        AdSettlementEntity entity = new AdSettlementEntity();
        entity.adId = adId;
        entity.totalAmount = totalAmount;
        entity.netAmount = netAmount;
        entity.status = SettlementStatus.COMPLETED;
        entity.settledAt = settledAt;
        entity.createdAt = settledAt;
        entity.updatedAt = settledAt;
        return entity;
    }
}
