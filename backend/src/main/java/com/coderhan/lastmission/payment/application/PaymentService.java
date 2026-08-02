package com.coderhan.lastmission.payment.application;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import com.coderhan.lastmission.marketing.BannerOrderDirectory;
import com.coderhan.lastmission.shared.order.OrderType;
import com.coderhan.lastmission.payment.domain.Payment;
import com.coderhan.lastmission.reservation.ReservationOrderDirectory;
import com.coderhan.lastmission.shared.error.BusinessException;
import com.coderhan.lastmission.shared.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository repository;
    private final PaymentGateway paymentGateway;
    private final ReservationOrderDirectory reservationOrderDirectory;
    private final BannerOrderDirectory bannerOrderDirectory;
    private final PaymentConfirmationRecorder confirmationRecorder;

    /**
     * 결제 승인. 별도의 "결제 신청" 흐름은 없음.
     *
     * amount는 클라이언트가 보낸 값을 그대로 신뢰하지 않고,
     * orderType에 따라 Reservation 또는 Marketing 쪽 주문 금액과 대조해 검증(불일치 시 PG 승인 호출 전에 거부)
     * - 클라이언트가 amount를 조작해 실제 주문 금액보다 적게 결제를 시도하는 것을 막기 위함.
     *
     * paymentKey를 idempotency_key로 재사용.
     * - 토스가 이미 결제 시도 1건당 고유하게 발급하는 값이라, 클라이언트가 별도로 idempotency key를 만들어 보낼 필요가 없다.
     *
     * confirm() 전체를 하나의 @Transactional로 묶지 않는다: 저장이 idempotency_key 유니크 제약
     * 위반으로 실패하면 CockroachDB/Postgres는 같은 트랜잭션 안의 이후 쿼리를 전부 거부하므로,
     * 실패 시 재조회는 반드시 새 트랜잭션에서 해야 한다. 저장 자체는 {@link PaymentConfirmationRecorder}의
     * 별도 트랜잭션으로 위임한다.
     */
    public Payment confirm(long userId, String orderId, OrderType orderType,
                           String pgOrderId, String paymentKey, BigDecimal amount) {
        Payment.validate(orderId, orderType, pgOrderId, paymentKey, amount);
        validateAmountMatchesOrder(orderId, orderType, amount);

        return repository.findByIdempotencyKey(paymentKey)
                .orElseGet(() ->
                    confirmAndSave(userId, orderId, orderType, pgOrderId, paymentKey, amount));
    }

    private void validateAmountMatchesOrder(String orderId, OrderType orderType, BigDecimal amount) {
        BigDecimal orderAmount = findOrderAmount(orderId, orderType)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_INVALID_REQUEST, "주문 내역을 찾을 수 없습니다."));

        if (amount.compareTo(orderAmount) != 0) {
            throw new BusinessException(ErrorCode.PAYMENT_INVALID_REQUEST, "결제 금액이 주문 금액과 일치하지 않습니다.");
        }
    }

    private Optional<BigDecimal> findOrderAmount(String orderId, OrderType orderType) {
        return switch (orderType) {
            case RESERVATION -> reservationOrderDirectory.findOrderAmount(orderId);
            case ADVERTISEMENT -> bannerOrderDirectory.findOrderAmount(orderId);
        };
    }

    private Payment confirmAndSave(long userId, String orderId, OrderType orderType,
                                   String pgOrderId, String paymentKey, BigDecimal amount) {
        PaymentGateway.ConfirmResult result = paymentGateway.confirm(paymentKey, pgOrderId, amount);

        try {
            /** 저장 + 이벤트 발행은 PaymentConfirmationRecorder의 @Transactional 메서드가 하나로 묶어서 처리한다. */
            return confirmationRecorder.save(
                    orderId, orderType, userId,
                    paymentKey, amount, result.method(),
                    "TOSS", pgOrderId, paymentKey,
                    result.approvedAt()
            );
        } catch (DataIntegrityViolationException e) {
            /** paymentKey(idempotency_key) 경합이면 먼저 커밋된 쪽을 반환.
             * 그게 아니라면 order_id 유니크 제약 위반 — 이 주문은 이미 다른 결제로 완료된 것이므로
             * 그대로 예외를 던진다(토스에는 이미 승인 요청을 보냈지만, 우리 쪽엔 저장하지 않는다).
             * confirmationRecorder.save()의 @Transactional이 예외 발생 시 트랜잭션을 롤백/종료한 뒤
             * 재던지므로, 아래 재조회는 항상 새 트랜잭션에서 실행된다(CockroachDB가 실패한 트랜잭션 안의
             * 후속 쿼리를 거부하는 문제 없음). */
            return repository.findByIdempotencyKey(paymentKey).orElseThrow(() -> e);
        }
    }

    public List<Payment> getMyPayments(long userId) {
        return repository.findByUserId(userId);
    }

    public Payment getPayment(long userId, long paymentId) {
        Payment payment = repository.findById(paymentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND, "결제 내역을 찾을 수 없습니다."));

        validatePaymentAccess(userId, payment);

        return payment;
    }

    private void validatePaymentAccess(long userId, Payment payment) {
        if (payment.userId() == null || payment.userId() != userId) {
            throw new BusinessException(ErrorCode.PAYMENT_ACCESS_DENIED, "본인의 결제 내역만 조회할 수 있습니다.");
        }
    }
}
