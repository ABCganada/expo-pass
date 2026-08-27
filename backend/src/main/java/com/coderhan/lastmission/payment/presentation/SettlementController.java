package com.coderhan.lastmission.payment.presentation;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import com.coderhan.lastmission.payment.application.SettlementPaymentDetail;
import com.coderhan.lastmission.payment.application.SettlementService;
import com.coderhan.lastmission.payment.domain.Payment;
import com.coderhan.lastmission.payment.domain.PaymentStatus;
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

    /**
     * 정산에 포함된 결제 내역 조회(감사용). 총매출 계산에 쓰인 것과 동일한 완료 결제 목록에
     * 각 결제의 활성 환불액(refundAmount)을 같이 내려준다 — 환불이 없으면 0.
     */
    @GetMapping("/{settlementId}/payments")
    ApiResponse<List<SettlementPaymentResponse>> payments(
            @PathVariable String settlementId,
            @AuthenticationPrincipal LastMissionPrincipal principal
    ) {
        List<SettlementPaymentResponse> payments = settlementService
                .getSettlementPayments(principal.userId(), parseSettlementId(settlementId)).stream()
                .map(SettlementPaymentResponse::from)
                .toList();

        return ApiResponse.success(payments);
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

    record SettlementPaymentResponse(
        String id, String orderId, BigDecimal amount, BigDecimal refundAmount, String method, PaymentStatus status,
        String pgProvider, String pgTransactionId, OffsetDateTime paidAt, OffsetDateTime createdAt
    ) {
        static SettlementPaymentResponse from(SettlementPaymentDetail detail) {
            Payment payment = detail.payment();
            return new SettlementPaymentResponse(Long.toString(payment.id()), payment.orderId(), payment.amount(),
                    detail.refundedAmount(), payment.method(), payment.status(), payment.pgProvider(),
                    payment.pgTransactionId(), payment.paidAt(), payment.createdAt());
        }
    }
}
