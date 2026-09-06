package com.coderhan.lastmission.payment.application;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
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
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository repository;
    private final PaymentGateway paymentGateway;
    private final ReservationOrderDirectory reservationOrderDirectory;
    private final BannerOrderDirectory bannerOrderDirectory;
    private final PaymentEventRecorder eventRecorder;
    private final Clock clock;

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
     * 실패 시 재조회는 반드시 새 트랜잭션에서 해야 한다. 저장 자체는 {@link PaymentEventRecorder}의
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

    /**
     * 결제 실패/취소 신고. PG 승인 호출이 없다 — 애초에 결제가 성사된 적이 없어서 승인 취소할 대상이
     * 없기 때문. 토스 결제위젯이 실패(failUrl)로 리다이렉트됐을 때 프론트가 호출해서, PENDING 상태로
     * 남아있는 주문에 실패를 알려준다.
     *
     * confirm()과 달리 클라이언트가 amount를 보내지 않는다 — 실제로 성사된 결제가 없어 검증할 대상이
     * 없기 때문. 대신 findOrderAmount()로 조회한 주문의 실제 금액을 그대로 감사 로그에 남긴다(클라이언트
     * 입력값을 신뢰하지 않는다는 원칙은 여기서도 동일). 저장 + 이벤트 발행은 {@link PaymentEventRecorder}에 위임한다.
     */
    public void reportFailure(long userId, String orderId, OrderType orderType, String reason) {
        validateOrderIdAndType(orderId, orderType);
        BigDecimal orderAmount = findOrderAmount(orderId, orderType)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_INVALID_REQUEST, "주문 내역을 찾을 수 없습니다."));

        eventRecorder.reportFailure(orderId, orderType, userId, orderAmount, reason, OffsetDateTime.now(clock));
    }

    private void validateOrderIdAndType(String orderId, OrderType orderType) {
        if (orderId == null || orderId.isBlank()) {
            throw new BusinessException(ErrorCode.PAYMENT_INVALID_REQUEST, "주문 ID가 올바르지 않습니다.");
        }
        if (orderType == null) {
            throw new BusinessException(ErrorCode.PAYMENT_INVALID_REQUEST, "주문 타입이 올바르지 않습니다.");
        }
    }

    private Payment confirmAndSave(long userId, String orderId, OrderType orderType,
                                   String pgOrderId, String paymentKey, BigDecimal amount) {
        PaymentGateway.ConfirmResult result = paymentGateway.confirm(paymentKey, pgOrderId, amount);

        return saveConfirmedPayment(orderId, orderType, userId, paymentKey, amount,
                result.method(), pgOrderId, result.approvedAt());
    }

    /** confirmAndSave()/reconcileApprovedPayment() 공통: 확보된 승인 정보를 저장 + 이벤트 발행한다. */
    private Payment saveConfirmedPayment(String orderId, OrderType orderType, Long userId,
                                         String paymentKey, BigDecimal amount, String method,
                                         String pgOrderId, OffsetDateTime approvedAt) {
        try {
            /** 저장 + 이벤트 발행은 PaymentEventRecorder의 @Transactional 메서드가 하나로 묶어서 처리한다. */
            return eventRecorder.reportConfirmation(
                    orderId, orderType, userId,
                    paymentKey, amount, method,
                    "TOSS", pgOrderId, paymentKey,
                    approvedAt
            );
        } catch (DataIntegrityViolationException e) {
            /** paymentKey(idempotency_key) 경합이면 먼저 커밋된 쪽을 반환.
             * 그게 아니라면 order_id 유니크 제약 위반 — 이 주문은 이미 다른 결제로 완료된 것이므로
             * 그대로 예외를 던진다(토스에는 이미 승인 요청을 보냈지만, 우리 쪽엔 저장하지 않는다).
             * eventRecorder.reportConfirmation()의 @Transactional이 예외 발생 시 트랜잭션을 롤백/종료한 뒤
             * 재던지므로, 아래 재조회는 항상 새 트랜잭션에서 실행된다(CockroachDB가 실패한 트랜잭션 안의
             * 후속 쿼리를 거부하는 문제 없음). */
            return repository.findByIdempotencyKey(paymentKey).orElseThrow(() -> e);
        }
    }

    /**
     * PG 승인은 이미 확인됐지만(payment_logs 재확인, #1) payments 저장이 유실된 결제를 토스를 다시
     * 호출하지 않고 사후 기록한다. userId는 호출 측이 모를 수 있어 nullable(결제 내역 조회에선 빠짐).
     */
    public Payment reconcileApprovedPayment(String orderId, OrderType orderType, Long userId,
                                            String paymentKey, BigDecimal amount, String method,
                                            String pgOrderId, OffsetDateTime approvedAt) {
        Payment.validate(orderId, orderType, pgOrderId, paymentKey, amount);
        validateAmountMatchesOrder(orderId, orderType, amount);

        return repository.findByIdempotencyKey(paymentKey)
                .orElseGet(() -> saveConfirmedPayment(orderId, orderType, userId, paymentKey, amount,
                        method, pgOrderId, approvedAt));
    }

    @Transactional(readOnly = true)
    public List<Payment> getMyPayments(long userId) {
        return repository.findByUserId(userId);
    }

    @Transactional(readOnly = true)
    public Payment getPayment(long userId, String orderId) {
        Payment payment = repository.findByOrderId(orderId)
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
