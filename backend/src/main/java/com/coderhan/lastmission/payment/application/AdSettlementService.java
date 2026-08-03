package com.coderhan.lastmission.payment.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.UUID;
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
}
