package com.coderhan.lastmission.payment.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import com.coderhan.lastmission.payment.domain.Payment;
import com.coderhan.lastmission.payment.domain.PaymentStatus;
import com.coderhan.lastmission.payment.domain.Refund;
import com.coderhan.lastmission.shared.error.BusinessException;
import com.coderhan.lastmission.shared.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RefundService {

    /** 환불율 구간 기준 (행사 시작까지 남은 일수, 이상 기준) */
    private static final long FULL_REFUND_MIN_DAYS_BEFORE_EVENT = 7;
    private static final long HALF_REFUND_MIN_DAYS_BEFORE_EVENT = 3;
    private static final long PARTIAL_REFUND_MIN_DAYS_BEFORE_EVENT = 1;

    private static final BigDecimal FULL_REFUND_RATE = new BigDecimal("1.00");
    private static final BigDecimal HALF_REFUND_RATE = new BigDecimal("0.50");
    private static final BigDecimal PARTIAL_REFUND_RATE = new BigDecimal("0.30");

    private final RefundRepository refundRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentGateway paymentGateway;
    private final EventScheduleReader eventScheduleReader;
    private final PaymentEventRecorder eventRecorder;
    private final Clock clock;

    /**
     * 환불 신청 접수. 모든 환불은 자동승인
     *
     * 행사 시작일까지 남은 일수에 따라 환불율이 정해진다(D-7 이상 100%, D-3~D-6 50%, D-1~D-2 30%).
     * 행사 시작일 당일(D-0) 이후는 환불율이 0%가 되는 게 아니라 환불 신청 자체를 거부한다 —
     * 0원짜리 환불을 그대로 접수하면 결제가 REFUNDED로 바뀌면서 정산 집계(COMPLETED 결제만 합산)에서
     * 통째로 빠져, 실제로는 한 푼도 안 돌려줬는데 주최자 매출이 사라지는 문제가 있었다.
     */
    public Refund request(long userId, long paymentId, String reason) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND, "결제 내역을 찾을 수 없습니다."));

        validateOwner(userId, payment);
        validateRefundable(payment);
        validateNoActiveRefund(paymentId);

        BigDecimal refundAmount = calculateRefundAmount(payment);

        return refund(payment, refundAmount, reason);
    }

    private BigDecimal calculateRefundAmount(Payment payment) {
        LocalDate eventStartDate = eventScheduleReader.findEventStartDate(payment.orderId());
        long daysUntilStart = ChronoUnit.DAYS.between(LocalDate.now(clock), eventStartDate);

        if (daysUntilStart < PARTIAL_REFUND_MIN_DAYS_BEFORE_EVENT) {
            throw new BusinessException(ErrorCode.PAYMENT_REFUND_NOT_ALLOWED,
                    "행사 시작일 이후에는 환불 신청을 할 수 없습니다.");
        }

        return payment.amount()
                .multiply(refundRate(daysUntilStart))
                .setScale(0, RoundingMode.HALF_UP);
    }

    private BigDecimal refundRate(long daysUntilStart) {
        if (daysUntilStart >= FULL_REFUND_MIN_DAYS_BEFORE_EVENT) {
            return FULL_REFUND_RATE;
        }
        if (daysUntilStart >= HALF_REFUND_MIN_DAYS_BEFORE_EVENT) {
            return HALF_REFUND_RATE;
        }
        return PARTIAL_REFUND_RATE;
    }

    private Refund refund(Payment payment, BigDecimal amount, String reason) {
        if (amount.signum() > 0) {
            paymentGateway.cancel(payment.pgTransactionId(), reason, amount);
        }

        try {
            /** 저장 + 이벤트 발행은 PaymentEventRecorder의 @Transactional 메서드가 하나로 묶어서 처리한다. */
            return eventRecorder.reportRefund(payment.id(), payment.orderId(), payment.orderType(), amount, reason,
                    OffsetDateTime.now(clock));
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.PAYMENT_REFUND_ALREADY_EXISTS, "이미 진행 중인 환불 신청이 있습니다.");
        }
    }

    private void validateOwner(long userId, Payment payment) {
        if (payment.userId() == null || payment.userId() != userId) {
            throw new BusinessException(ErrorCode.PAYMENT_ACCESS_DENIED, "본인의 결제 내역만 환불 신청할 수 있습니다.");
        }
    }

    private void validateRefundable(Payment payment) {
        if (payment.status() != PaymentStatus.COMPLETED) {
            throw new BusinessException(ErrorCode.PAYMENT_REFUND_NOT_ALLOWED, "완료된 결제만 환불 신청할 수 있습니다.");
        }
    }

    private void validateNoActiveRefund(long paymentId) {
        if (refundRepository.findActiveByPaymentId(paymentId).isPresent()) {
            throw new BusinessException(ErrorCode.PAYMENT_REFUND_ALREADY_EXISTS, "이미 진행 중인 환불 신청이 있습니다.");
        }
    }
}
