package com.coderhan.lastmission.payment.presentation;

import com.coderhan.lastmission.payment.application.PaymentService;
import com.coderhan.lastmission.payment.application.RefundService;
import com.coderhan.lastmission.payment.domain.OrderType;
import com.coderhan.lastmission.payment.domain.Payment;
import com.coderhan.lastmission.payment.domain.PaymentStatus;
import com.coderhan.lastmission.payment.domain.Refund;
import com.coderhan.lastmission.payment.domain.RefundStatus;
import com.coderhan.lastmission.shared.ApiResponse;
import com.coderhan.lastmission.shared.error.BusinessException;
import com.coderhan.lastmission.shared.error.ErrorCode;
import com.coderhan.lastmission.user.LastMissionPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
class PaymentController {

    private final PaymentService paymentService;
    private final RefundService refundService;

    /**
     * 결제 승인 API. 별도의 "결제 신청" API는 없음
     * 예약 주문 생성/광고 등록이 결제 신청 역할을 하고, API는 토스 결제위젯 완료 후 승인만 담당한다.
     *
     * 경로변수 {@code orderId}는 우리 쪽 주문 ID(payments.order_id) — 바디의 orderType에 따라
     * 예약 주문(reservation_orders) 또는 광고 주문(marketing_banner_ads)을 가리킨다.
     * 바디의 paymentKey/pgOrderId/amount는 토스 결제위젯이 구매자 인증 완료 후 successUrl로
     * 돌려주는 값 3개를 그대로 받는다 — pgOrderId는 우리가 위젯을 열 때 토스에 넘긴 값이라
     * orderId와 다를 수 있다(재시도 시 접미사 등).
     *
     * TODO orderType에 따른 승인 분기(예약/광고 각각의 금액 검증)는 아직 미구현 — 현재는 예약 주문만 처리.
     * TODO 결제 승인/실패에 대한 알림 reservation/marketing에 줘야 PENDING 변경 가능
     * TODO 결제 - 오더 간 정합성 스케줄링
     */
    @PostMapping("/{orderId}/confirm")
    ResponseEntity<ApiResponse<PaymentResponse>> confirm(
        @PathVariable String orderId,
        @RequestBody PaymentConfirmRequest request,
        @AuthenticationPrincipal LastMissionPrincipal principal
    ) {
        Payment payment = paymentService.confirm(principal.userId(), orderId, request.orderType(),
                request.pgOrderId(), request.paymentKey(), request.amount());

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(ApiResponse.success(PaymentResponse.from(payment)));
    }

    /** 내 결제 내역 목록 조회. 최신순 */
    @GetMapping
    ApiResponse<List<PaymentResponse>> myPayments(@AuthenticationPrincipal LastMissionPrincipal principal) {
        List<PaymentResponse> payments = paymentService.getMyPayments(principal.userId()).stream()
                .map(PaymentResponse::from)
                .toList();

        return ApiResponse.success(payments);
    }

    /** 결제 내역 상세 조회. 본인 것만 조회 가능. */
    @GetMapping("/{paymentId}")
    ApiResponse<PaymentResponse> getPayment(
        @PathVariable String paymentId,
        @AuthenticationPrincipal LastMissionPrincipal principal
    ) {
        Payment payment = paymentService.getPayment(principal.userId(), parsePaymentId(paymentId));
        return ApiResponse.success(PaymentResponse.from(payment));
    }

    /** 환불 신청 접수. 모든 환불은 자동승인되며, 행사 시작까지 남은 일수에 따라 환불율이 정해진다. */
    @PostMapping("/{paymentId}/refunds")
    ResponseEntity<ApiResponse<RefundResponse>> request(
            @PathVariable String paymentId,
            @RequestBody RefundRequest request,
            @AuthenticationPrincipal LastMissionPrincipal principal
    ) {
        Refund refund = refundService.request(principal.userId(), parsePaymentId(paymentId), request.reason());

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(RefundResponse.from(refund)));
    }

    private long parsePaymentId(String value) {
        try {
            long id = Long.parseLong(value);
            if (id <= 0) throw new NumberFormatException();
            return id;
        } catch (NumberFormatException exception) {
            throw new BusinessException(ErrorCode.PAYMENT_NOT_FOUND, "결제 내역을 찾을 수 없습니다.");
        }
    }

    record PaymentConfirmRequest(
        OrderType orderType,
        String pgOrderId,
        String paymentKey,
        BigDecimal amount
    ) {}

    record RefundRequest(String reason) {}

    record RefundResponse(
        String id, String paymentId, BigDecimal amount,
        String reason, RefundStatus status, boolean autoApproved,
        OffsetDateTime requestedAt, OffsetDateTime createdAt
    ) {
        static RefundResponse from(Refund refund) {
            return new RefundResponse(Long.toString(refund.id()), Long.toString(refund.paymentId()),
                    refund.amount(), refund.reason(), refund.status(), refund.autoApproved(),
                    refund.requestedAt(), refund.createdAt());
        }
    }

    record PaymentResponse(
        String id, String orderId, String idempotencyKey,
        BigDecimal amount, String method, PaymentStatus status,
        String pgProvider, String pgOrderId, String pgTransactionId,
        OffsetDateTime paidAt, OffsetDateTime createdAt
    ) {
        static PaymentResponse from(Payment payment) {
            return new PaymentResponse(Long.toString(payment.id()), payment.orderId(), payment.idempotencyKey(),
                    payment.amount(), payment.method(), payment.status(), payment.pgProvider(),
                    payment.pgOrderId(), payment.pgTransactionId(), payment.paidAt(), payment.createdAt());
        }
    }
}
