package com.coderhan.lastmission.payment.presentation;

import java.math.BigDecimal;
import com.coderhan.lastmission.payment.application.SettlementService;
import com.coderhan.lastmission.payment.domain.SettlementSummary;
import com.coderhan.lastmission.shared.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/payments")
@RequiredArgsConstructor
class SettlementAdminController {

    private final SettlementService settlementService;

    /** 전체 매출 대시보드 조회. 플랫폼 전체 정산 합계를 보여준다. */
    @GetMapping("/dashboard")
    ApiResponse<DashboardResponse> dashboard() {
        return ApiResponse.success(DashboardResponse.from(settlementService.getDashboardSummary()));
    }

    record DashboardResponse(
        BigDecimal totalSales, BigDecimal totalCommissionAmount, long settlementCount
    ) {
        static DashboardResponse from(SettlementSummary summary) {
            return new DashboardResponse(summary.totalSales(), summary.totalCommissionAmount(),
                    summary.settlementCount());
        }
    }
}
