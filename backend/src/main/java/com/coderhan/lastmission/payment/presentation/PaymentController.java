package com.coderhan.lastmission.payment.presentation;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import com.coderhan.lastmission.payment.application.PaymentService;
import com.coderhan.lastmission.payment.domain.Payment;
import com.coderhan.lastmission.payment.domain.PaymentStatus;
import com.coderhan.lastmission.shared.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/payments")
@RequiredArgsConstructor
class PaymentController {

    private final PaymentService paymentService;

    /**
     * 결제 승인 API. Payment 도메인이 제공하는 유일한 쓰기 API다 — 별도의 "결제 신청" API는 없다.
     * 예약 주문 생성이 사실상 신청 역할을 하고, 이 API는 토스 결제위젯 완료 후 승인만 담당한다.
     *
     * 경로변수 {@code reservationOrderId}는 우리 쪽 예약 주문 ID(payments.order_id).
     * 바디의 paymentKey/orderId(=payments.pg_order_id)/amount는 토스 결제위젯이 구매자 인증
     * 완료 후 successUrl로 돌려주는 값 3개를 그대로 받는다 — orderId는 우리가 위젯을 열 때
     * 토스에 넘긴 값이라 reservationOrderId와 다를 수 있다(재시도 시 접미사 등).
     *
     * amount는 Reservation과 대조하지 않고 클라이언트가 보낸 값을 그대로 신뢰한다(팀 결정).
     */
    @PostMapping("/{reservationOrderId}/confirm")
    ResponseEntity<ApiResponse<PaymentResponse>> confirm(
        @PathVariable String reservationOrderId,
        @RequestBody PaymentConfirmRequest request
    ) {
        Payment payment = paymentService.confirm(reservationOrderId, request.pgOrderId(), request.paymentKey(),
                request.amount());

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(ApiResponse.success(PaymentResponse.from(payment)));
    }

    record PaymentConfirmRequest(String pgOrderId, String paymentKey, BigDecimal amount) {}

    record PaymentResponse(
        String id, String orderId, String idempotencyKey,
        BigDecimal amount, String method, PaymentStatus status,
        String pgProvider, String pgTransactionId, OffsetDateTime paidAt, OffsetDateTime createdAt
    ) {
        static PaymentResponse from(Payment payment) {
            return new PaymentResponse(Long.toString(payment.id()), payment.orderId(), payment.idempotencyKey(),
                    payment.amount(), payment.method(), payment.status(), payment.pgProvider(),
                    payment.pgTransactionId(), payment.paidAt(), payment.createdAt());
        }
    }
}
