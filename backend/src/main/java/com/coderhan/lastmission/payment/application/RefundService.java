package com.coderhan.lastmission.payment.application;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Objects;
import com.coderhan.lastmission.payment.domain.Payment;
import com.coderhan.lastmission.payment.domain.PaymentStatus;
import com.coderhan.lastmission.payment.domain.Refund;
import com.coderhan.lastmission.payment.domain.RefundStatus;
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
    private static final BigDecimal NO_REFUND_RATE = BigDecimal.ZERO;

    private final RefundRepository refundRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentGateway paymentGateway;
    private final EventScheduleReader eventScheduleReader;
    private final EventManagerLookup eventManagerLookup;
    private final Clock clock;

    /**
     * 환불 신청 접수. 모든 환불은 자동승인
     *
     * 행사 시작일까지 남은 일수에 따라 환불율이 정해진다(D-7 이상 100%, D-3~D-6 50%, D-1~D-2 30%, 당일(D-0) 0%)
     * 환불액이 0원인 경우에도 예약 취소 이력을 남기기 위해 Refund는 그대로 생성하되, 토스 결제취소는호출하지 않는다.
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
        if (daysUntilStart >= PARTIAL_REFUND_MIN_DAYS_BEFORE_EVENT) {
            return PARTIAL_REFUND_RATE;
        }
        return NO_REFUND_RATE;
    }

    private Refund refund(Payment payment, BigDecimal amount, String reason) {
        if (amount.signum() > 0) {
            paymentGateway.cancel(payment.pgTransactionId(), reason);
        }

        try {
            return refundRepository.save(payment.id(), amount, reason, OffsetDateTime.now(clock));
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.PAYMENT_REFUND_ALREADY_EXISTS, "이미 진행 중인 환불 신청이 있습니다.");
        }
    }

    /** 이벤트 관리자의 환불 승인. REQUESTED 상태인 신청 중 본인이 담당하는 행사의 건만 승인 가능하다. */
    public Refund approve(long approverUserId, long refundId) {
        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_REFUND_NOT_FOUND, "환불 신청 내역을 찾을 수 없습니다."));

        validateStatusRequested(refund);

        Payment payment = paymentRepository.findById(refund.paymentId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND, "결제 내역을 찾을 수 없습니다."));

        validateEventAccess(payment, approverUserId);

        paymentGateway.cancel(payment.pgTransactionId(), refund.reason());

        return refundRepository.approve(refundId, approverUserId, OffsetDateTime.now(clock));
    }

    /**
     * 이벤트 관리자의 환불 거절. REQUESTED 상태인 신청 중 본인이 담당하는 행사의 건만
     * 거절 가능하다. 실제 결제취소는 호출하지 않는다.
     */
    public Refund reject(long approverUserId, long refundId) {
        Refund refund = refundRepository.findById(refundId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_REFUND_NOT_FOUND, "환불 신청 내역을 찾을 수 없습니다."));

        validateStatusRequested(refund);

        Payment payment = paymentRepository.findById(refund.paymentId())
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND, "결제 내역을 찾을 수 없습니다."));

        validateEventAccess(payment, approverUserId);

        return refundRepository.reject(refundId, approverUserId, OffsetDateTime.now(clock));
    }

    /** 이벤트 관리자가 승인/거절해야 할 대기 목록. 본인이 담당하는 행사의 환불만 보인다. */
    public List<Refund> listPending(long callerUserId) {
        return refundRepository.findAllRequested().stream()
                .filter(refund -> isManagedByCaller(refund, callerUserId))
                .toList();
    }

    private boolean isManagedByCaller(Refund refund, long callerUserId) {
        return paymentRepository.findById(refund.paymentId())
                .map(payment -> Objects.equals(eventManagerLookup.findEventManagerId(payment.orderId()), callerUserId))
                .orElse(false);
    }

    private void validateEventAccess(Payment payment, long callerUserId) {
        Long managerId = eventManagerLookup.findEventManagerId(payment.orderId());
        if (!Objects.equals(managerId, callerUserId)) {
            throw new BusinessException(ErrorCode.PAYMENT_REFUND_ACCESS_DENIED, "본인이 담당하는 행사의 환불만 처리할 수 있습니다.");
        }
    }

    private void validateStatusRequested(Refund refund) {
        if (refund.status() != RefundStatus.REQUESTED) {
            throw new BusinessException(ErrorCode.PAYMENT_REFUND_ALREADY_DECIDED, "이미 처리된 환불 신청입니다.");
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
