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
     * 결제 요청 API.
     *
     * 주의(임시): 아직 reservation의 order_id 존재/소유자/상태를 검증하지 않고
     * amount도 클라이언트가 보낸 값을 그대로 사용한다.
     * TODO Reservation 연동은 별도 작업으로 진행 예정 — 그 전까지는 뼈대만 갖춘 상태
     */
    @PostMapping
    ResponseEntity<ApiResponse<PaymentResponse>> pay(@RequestBody PaymentRequest request) {
        Payment payment = paymentService.pay(request.orderId(), request.idempotencyKey(),
                request.amount(), request.method(), request.pgProvider());

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(ApiResponse.success(PaymentResponse.from(payment)));
    }

    record PaymentRequest(
        String orderId, String idempotencyKey, BigDecimal amount,
        String method, String pgProvider
    ) {}

    record PaymentResponse(
        String id, String orderId, String idempotencyKey,
        BigDecimal amount, String method, PaymentStatus status,
        String pgProvider, OffsetDateTime createdAt
    ) {
        static PaymentResponse from(Payment payment) {
            return new PaymentResponse(Long.toString(payment.id()), payment.orderId(), payment.idempotencyKey(),
                    payment.amount(), payment.method(), payment.status(), payment.pgProvider(),
                    payment.createdAt());
        }
    }
}
