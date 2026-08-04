package com.coderhan.lastmission.payment.application;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import com.coderhan.lastmission.payment.domain.AdSettlement;
import com.coderhan.lastmission.payment.domain.AdSettlementSummary;

public interface AdSettlementRepository {

    /** 광고 1건당 정산이 이미 있는지 조회 — 중복 생성 방지용 */
    boolean existsByAdId(UUID adId);

    /** 정산 생성 */
    AdSettlement save(UUID adId, BigDecimal totalAmount, BigDecimal commissionRate,
                    BigDecimal commissionAmount, BigDecimal netAmount, OffsetDateTime settledAt);

    /** 전체 광고 정산 목록. 최신순(확정 시각 내림차순) */
    List<AdSettlement> findAll();

    Optional<AdSettlement> findById(long adSettlementId);

    /** 전체 광고 정산 합계 (플랫폼 관리자 - 전체 매출 대시보드용) */
    AdSettlementSummary getDashboardSummary();
}
