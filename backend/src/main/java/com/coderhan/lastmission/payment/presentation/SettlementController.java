package com.coderhan.lastmission.payment.presentation;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import com.coderhan.lastmission.payment.application.SettlementService;
import com.coderhan.lastmission.payment.domain.Settlement;
import com.coderhan.lastmission.payment.domain.SettlementStatus;
import com.coderhan.lastmission.shared.ApiResponse;
import com.coderhan.lastmission.shared.error.BusinessException;
import com.coderhan.lastmission.shared.error.ErrorCode;
import com.coderhan.lastmission.user.LastMissionPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/manager/payments/settlements")
@RequiredArgsConstructor
class SettlementController {

    private final SettlementService settlementService;

    /** 정산 목록 조회. 본인이 담당하는 행사의 정산만 보인다. */
    @GetMapping
    ApiResponse<List<SettlementResponse>> list(@AuthenticationPrincipal LastMissionPrincipal principal) {
        List<SettlementResponse> settlements = settlementService.list(principal.userId()).stream()
                .map(SettlementResponse::from)
                .toList();

        return ApiResponse.success(settlements);
    }

    /** 정산 상세 조회. 본인이 담당하는 행사의 정산만 조회할 수 있다. */
    @GetMapping("/{settlementId}")
    ApiResponse<SettlementResponse> get(
            @PathVariable String settlementId,
            @AuthenticationPrincipal LastMissionPrincipal principal
    ) {
        Settlement settlement = settlementService.get(principal.userId(), parseSettlementId(settlementId));

        return ApiResponse.success(SettlementResponse.from(settlement));
    }

    private long parseSettlementId(String value) {
        try {
            long id = Long.parseLong(value);
            if (id <= 0) throw new NumberFormatException();
            return id;
        } catch (NumberFormatException exception) {
            throw new BusinessException(ErrorCode.PAYMENT_SETTLEMENT_NOT_FOUND, "정산 내역을 찾을 수 없습니다.");
        }
    }

    record SettlementResponse(
        String id, String eventId, BigDecimal totalSales, BigDecimal commissionRate,
        BigDecimal commissionAmount, BigDecimal netAmount, SettlementStatus status,
        OffsetDateTime settledAt, OffsetDateTime createdAt
    ) {
        static SettlementResponse from(Settlement settlement) {
            return new SettlementResponse(Long.toString(settlement.id()), Long.toString(settlement.eventId()),
                    settlement.totalSales(), settlement.commissionRate(), settlement.commissionAmount(),
                    settlement.netAmount(), settlement.status(), settlement.settledAt(), settlement.createdAt());
        }
    }
}
