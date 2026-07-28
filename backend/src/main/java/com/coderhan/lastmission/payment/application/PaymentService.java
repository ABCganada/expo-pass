package com.coderhan.lastmission.payment.application;

import java.math.BigDecimal;
import com.coderhan.lastmission.payment.domain.Payment;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository repository;
    private final PaymentGateway paymentGateway;

    /**
     * 결제 승인. Payment 도메인의 유일한 쓰기 로직이다 — 별도의 "결제 신청" 흐름은 없다.
     *
     * Reservation 조회 없이 클라이언트가 보낸 amount를 그대로 신뢰한다(팀 결정).
     * paymentKey를 idempotency_key로 재사용
     * 토스가 이미 결제 시도 1건당 고유하게 발급하는 값이라, 클라이언트가 별도로 idempotency key를 만들어 보낼 필요가 없다.
     *
     * 서비스 메서드 전체를 하나의 @Transactional로 묶지 않는다: save()가 idempotency_key
     * 유니크 제약 위반으로 실패하면 CockroachDB/Postgres는 같은 트랜잭션 안의 이후 쿼리를 전부
     * 거부하므로, 실패 시 재조회는 반드시 새 트랜잭션(Spring Data 기본 메서드별 트랜잭션)에서 해야 한다.
     */
    public Payment confirm(String reservationOrderId, String pgOrderId, String paymentKey,
                           BigDecimal amount) {
        Payment.validate(reservationOrderId, pgOrderId, paymentKey, amount);

        return repository.findByIdempotencyKey(paymentKey)
                .orElseGet(() -> confirmAndSave(reservationOrderId, pgOrderId, paymentKey, amount));
    }

    private Payment confirmAndSave(String reservationOrderId, String pgOrderId, String paymentKey,
                                   BigDecimal amount) {
        PaymentGateway.ConfirmResult result = paymentGateway.confirm(paymentKey, pgOrderId, amount);

        try {
            return repository.save(reservationOrderId, paymentKey, amount, result.method(), "TOSS",
                    pgOrderId, paymentKey, result.approvedAt());
        } catch (DataIntegrityViolationException e) {
            /** paymentKey(idempotency_key) 경합이면 먼저 커밋된 쪽을 반환.
             * 그게 아니라면 order_id 유니크 제약 위반 — 이 주문은 이미 다른 결제로 완료된 것이므로
             * 그대로 예외를 던진다(토스에는 이미 승인 요청을 보냈지만, 우리 쪽엔 저장하지 않는다). */
            return repository.findByIdempotencyKey(paymentKey).orElseThrow(() -> e);
        }
    }
}
