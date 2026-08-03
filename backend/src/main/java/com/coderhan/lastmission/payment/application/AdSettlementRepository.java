package com.coderhan.lastmission.payment.application;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
import com.coderhan.lastmission.payment.domain.AdSettlement;

public interface AdSettlementRepository {

    /** 광고 1건당 정산이 이미 있는지 조회 — 중복 생성 방지용 */
    boolean existsByAdId(UUID adId);

    /** 정산 생성 */
    AdSettlement save(UUID adId, BigDecimal totalAmount, BigDecimal commissionRate,
                    BigDecimal commissionAmount, BigDecimal netAmount, OffsetDateTime settledAt);
}
