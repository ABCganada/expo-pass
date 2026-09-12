package com.coderhan.lastmission.payment.presentation;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import com.coderhan.lastmission.payment.application.AdSettlementService;
import com.coderhan.lastmission.payment.domain.AdSettlement;
import com.coderhan.lastmission.payment.domain.AdSettlementSummary;
import com.coderhan.lastmission.payment.domain.SettlementStatus;
import com.coderhan.lastmission.shared.ApiResponse;
import com.coderhan.lastmission.shared.error.BusinessException;
import com.coderhan.lastmission.shared.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/payments/ad-settlements")
@RequiredArgsConstructor
class AdSettlementAdminController {

    private final AdSettlementService adSettlementService;

    /** 광고 정산 목록 조회 (ADMIN 전용, 소유자별 필터링 없음). */
    @GetMapping
    ApiResponse<List<AdSettlementResponse>> list() {
        List<AdSettlementResponse> settlements = adSettlementService.list().stream()
                .map(AdSettlementResponse::from)
                .toList();

        return ApiResponse.success(settlements);
    }

    /** 광고 정산 상세 조회 (ADMIN 전용). */
    @GetMapping("/{adSettlementId}")
    ApiResponse<AdSettlementResponse> get(@PathVariable String adSettlementId) {
        AdSettlement settlement = adSettlementService.get(parseAdSettlementId(adSettlementId));

        return ApiResponse.success(AdSettlementResponse.from(settlement));
    }

    /** 전체 광고 매출 대시보드 조회. 플랫폼 전체 광고 정산 합계를 보여준다. */
    @GetMapping("/dashboard")
    ApiResponse<DashboardResponse> dashboard() {
        return ApiResponse.success(DashboardResponse.from(adSettlementService.getDashboardSummary()));
    }

    private long parseAdSettlementId(String value) {
        try {
            long id = Long.parseLong(value);
            if (id <= 0) throw new NumberFormatException();
            return id;
        } catch (NumberFormatException exception) {
            throw new BusinessException(ErrorCode.PAYMENT_AD_SETTLEMENT_NOT_FOUND, "광고 정산 내역을 찾을 수 없습니다.");
        }
    }

    record AdSettlementResponse(
        String id, String adId, BigDecimal totalAmount, BigDecimal netAmount,
        SettlementStatus status, OffsetDateTime settledAt, OffsetDateTime createdAt
    ) {
        static AdSettlementResponse from(AdSettlement settlement) {
            return new AdSettlementResponse(Long.toString(settlement.id()), settlement.adId().toString(),
                    settlement.totalAmount(), settlement.netAmount(), settlement.status(), settlement.settledAt(),
                    settlement.createdAt());
        }
    }

    record DashboardResponse(
        BigDecimal totalAmount,
        BigDecimal totalNetAmount,
        long settlementCount
    ) {
        static DashboardResponse from(AdSettlementSummary summary) {
            return new DashboardResponse(
                summary.totalAmount(),
                summary.totalNetAmount(),
                summary.settlementCount());
        }
    }
}
