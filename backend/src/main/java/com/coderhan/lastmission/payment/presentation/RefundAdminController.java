package com.coderhan.lastmission.payment.presentation;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import com.coderhan.lastmission.payment.application.RefundService;
import com.coderhan.lastmission.payment.domain.Refund;
import com.coderhan.lastmission.payment.domain.RefundStatus;
import com.coderhan.lastmission.shared.ApiResponse;
import com.coderhan.lastmission.shared.error.BusinessException;
import com.coderhan.lastmission.shared.error.ErrorCode;
import com.coderhan.lastmission.user.LastMissionPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/manager/payments/refunds")
@RequiredArgsConstructor
class RefundAdminController {

    private final RefundService refundService;

    @PatchMapping("/{refundId}/approve")
    ApiResponse<RefundResponse> approve(
            @PathVariable String refundId,
            @AuthenticationPrincipal LastMissionPrincipal principal
    ) {
        Refund refund = refundService.approve(principal.userId(), parseRefundId(refundId));
        return ApiResponse.success(RefundResponse.from(refund));
    }

    @PatchMapping("/{refundId}/reject")
    ApiResponse<RefundResponse> reject(
            @PathVariable String refundId,
            @AuthenticationPrincipal LastMissionPrincipal principal
    ) {
        Refund refund = refundService.reject(principal.userId(), parseRefundId(refundId));
        return ApiResponse.success(RefundResponse.from(refund));
    }

    private long parseRefundId(String value) {
        try {
            long id = Long.parseLong(value);
            if (id <= 0) throw new NumberFormatException();
            return id;
        } catch (NumberFormatException exception) {
            throw new BusinessException(ErrorCode.PAYMENT_REFUND_NOT_FOUND, "환불 신청 내역을 찾을 수 없습니다.");
        }
    }

    record RefundResponse(
        String id, String paymentId, BigDecimal amount, String reason,
        RefundStatus status, boolean autoApproved, Long approvedBy,
        OffsetDateTime requestedAt, OffsetDateTime refundedAt
    ) {
        static RefundResponse from(Refund refund) {
            return new RefundResponse(Long.toString(refund.id()), Long.toString(refund.paymentId()),
                    refund.amount(), refund.reason(), refund.status(), refund.autoApproved(),
                    refund.approvedBy(), refund.requestedAt(), refund.refundedAt());
        }
    }
}
