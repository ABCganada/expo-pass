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

    /** 서비스 메서드 전체를 하나의 @Transactional로 묶지 않는다
     * save()가 idempotency_key 유니크 제약 위반으로 실패하면 CockroachDB/Postgres는 같은 트랜잭션 안의 이후 쿼리를
     * 전부 거부하므로, 실패 시 재조회는 반드시 새 트랜잭션(Spring Data 기본 메서드별 트랜잭션)에서 해야 한다. */
    public Payment pay(
        String orderId, String idempotencyKey, BigDecimal amount,
        String method, String pgProvider
    ) {
        Payment.validate(orderId, idempotencyKey, amount, method, pgProvider);

        return repository.findByIdempotencyKey(idempotencyKey)
                .orElseGet(() -> createOrReturnExisting(orderId, idempotencyKey, amount, method, pgProvider));
    }

    private Payment createOrReturnExisting(
        String orderId, String idempotencyKey, BigDecimal amount,
        String method, String pgProvider
    ) {
        try {
            return repository.save(orderId, idempotencyKey, amount, method, pgProvider);
        } catch (DataIntegrityViolationException e) {
            /** 동시에 같은 idempotency_key로 두 요청이 들어와 둘 다 findByIdempotencyKey에서
             * "없음"으로 판단한 뒤 save()가 경합한 경우 — 먼저 커밋된 쪽을 그대로 반환한다. */
            return repository.findByIdempotencyKey(idempotencyKey).orElseThrow(() -> e);
        }
    }
}
