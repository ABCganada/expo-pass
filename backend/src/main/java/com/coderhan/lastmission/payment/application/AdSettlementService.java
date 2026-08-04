package com.coderhan.lastmission.payment.application;

import java.math.BigDecimal;
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
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AdSettlementService {

    private final AdSettlementRepository adSettlementRepository;
    private final Clock clock;

    /**
     * 광고 만료 시 정산 생성.
     * 이미 생성된 정산이 있으면 아무 것도 하지 않는다(멱등) - 광고 만료 이벤트가 중복 발행/재처리돼도 정산이 두 번 생기지 않게 하기 위함.
     *
     * 광고는 박람회 관리자가 플랫폼에 직접 결제하는 구조라 수수료 개념이 없다 — 결제 금액 전액이
     * 그대로 순매출로 잡힌다(행사 티켓 정산과 달리 대행 수수료를 뗄 대상이 없음).
     *
     * REQUIRES_NEW: AdSettlementEventListener가 AFTER_COMMIT에서 호출한다(SettlementService.create()와
     * 동일한 이유). REQUIRED로 두면 원래 트랜잭션이 이미 끝난 컨텍스트에 join되어, save() 직후
     * findByAdId() 재조회가 방금 커밋한 데이터를 못 찾는다.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void create(UUID adId, long totalAmount) {
        if (adSettlementRepository.existsByAdId(adId)) {
            return;
        }

        BigDecimal totalAmountDecimal = BigDecimal.valueOf(totalAmount);

        adSettlementRepository.save(adId, totalAmountDecimal, totalAmountDecimal, OffsetDateTime.now(clock));
    }

    /** 광고 정산 목록 조회 (ADMIN 전용 — 소유자별 필터링 없음). */
    @Transactional(readOnly = true)
    public List<AdSettlement> list() {
        return adSettlementRepository.findAll();
    }

    /** 광고 정산 상세 조회 (ADMIN 전용). */
    @Transactional(readOnly = true)
    public AdSettlement get(long adSettlementId) {
        return adSettlementRepository.findById(adSettlementId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_AD_SETTLEMENT_NOT_FOUND, "광고 정산 내역을 찾을 수 없습니다."));
    }

    /** 전체 광고 매출 대시보드 조회. 전체 광고 정산을 합산한다(ADMIN 전용). */
    @Transactional(readOnly = true)
    public AdSettlementSummary getDashboardSummary() {
        return adSettlementRepository.getDashboardSummary();
    }
}
