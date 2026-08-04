package com.coderhan.lastmission.payment.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import com.coderhan.lastmission.payment.domain.AdSettlement;
import com.coderhan.lastmission.payment.domain.AdSettlementSummary;
import com.coderhan.lastmission.shared.error.BusinessException;
import com.coderhan.lastmission.shared.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AdSettlementService {

    private static final BigDecimal COMMISSION_RATE_PERCENT = new BigDecimal("5.00");
    private static final BigDecimal PERCENT_DIVISOR = new BigDecimal("100");

    private final AdSettlementRepository adSettlementRepository;
    private final Clock clock;

    /**
     * 광고 만료 시 정산 생성.
     * 이미 생성된 정산이 있으면 아무 것도 하지 않는다(멱등) - 광고 만료 이벤트가 중복 발행/재처리돼도 정산이 두 번 생기지 않게 하기 위함.
     */
    public void create(UUID adId, long totalAmount) {
        if (adSettlementRepository.existsByAdId(adId)) {
            return;
        }

        BigDecimal totalAmountDecimal = BigDecimal.valueOf(totalAmount);
        BigDecimal commissionAmount = totalAmountDecimal
                .multiply(COMMISSION_RATE_PERCENT)
                .divide(PERCENT_DIVISOR, 0, RoundingMode.HALF_UP);
        BigDecimal netAmount = totalAmountDecimal.subtract(commissionAmount);

        adSettlementRepository.save(adId, totalAmountDecimal, COMMISSION_RATE_PERCENT,
            commissionAmount, netAmount, OffsetDateTime.now(clock));
    }

    /** 광고 정산 목록 조회 (ADMIN 전용 — 소유자별 필터링 없음). */
    public List<AdSettlement> list() {
        return adSettlementRepository.findAll();
    }

    /** 광고 정산 상세 조회 (ADMIN 전용). */
    public AdSettlement get(long adSettlementId) {
        return adSettlementRepository.findById(adSettlementId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_AD_SETTLEMENT_NOT_FOUND, "광고 정산 내역을 찾을 수 없습니다."));
    }

    /** 전체 광고 매출 대시보드 조회. 전체 광고 정산을 합산한다(ADMIN 전용). */
    public AdSettlementSummary getDashboardSummary() {
        return adSettlementRepository.getDashboardSummary();
    }
}
