package com.coderhan.lastmission.payment.application;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import com.coderhan.lastmission.payment.domain.Settlement;

public interface SettlementRepository {

    /** 행사 1건당 정산이 이미 있는지 조회 — 중복 생성 방지용 */
    boolean existsByEventId(long eventId);

    /** 정산 생성 */
    Settlement save(long eventId, BigDecimal totalSales, BigDecimal commissionRate,
                    BigDecimal commissionAmount, BigDecimal netAmount, OffsetDateTime settledAt);

    /** 주어진 행사 id들에 해당하는 정산 목록. 최신순(확정 시각 내림차순) */
    List<Settlement> findByEventIdIn(List<Long> eventIds);

    Optional<Settlement> findById(long settlementId);
}
