package com.coderhan.lastmission.payment.application;

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

    /** 행사 시작 D-3 이전 신청은 자동승인 */
    private static final long AUTO_APPROVAL_MIN_DAYS_BEFORE_EVENT = 3;

    private final RefundRepository refundRepository;
    private final PaymentRepository paymentRepository;
    private final PaymentGateway paymentGateway;
    private final EventScheduleReader eventScheduleReader;
    private final Clock clock;

    /**
     * 환불 신청 접수. 환불 금액은 항상 결제 전액
     *
     * 행사 시작일 기준 D-3 이전 신청이면 자동승인 — 토스 결제취소를 이 메서드 안에서 즉시
     * 호출하고 COMPLETED로 저장한다. D-3 이내 신청이면 REQUESTED로만 접수하고, 승인/거절은
     * 이벤트 관리자가 별도 화면에서 처리한다(그 API는 이번 스코프에 포함되지 않는다).
     */
    public Refund request(long userId, long paymentId, String reason) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND, "결제 내역을 찾을 수 없습니다."));

        validateOwner(userId, payment);
        validateRefundable(payment);
        validateNoActiveRefund(paymentId);

        if (canAutoApprove(payment.orderId())) {
            return refund(payment, reason);
        }

        return saveAsRequested(payment, reason);
    }

    private boolean canAutoApprove(String orderId) {
        LocalDate eventStartDate = eventScheduleReader.findEventStartDate(orderId);
        long daysUntilStart = ChronoUnit.DAYS.between(LocalDate.now(clock), eventStartDate);

        return daysUntilStart >= AUTO_APPROVAL_MIN_DAYS_BEFORE_EVENT;
    }

    private Refund refund(Payment payment, String reason) {
        paymentGateway.cancel(payment.pgTransactionId(), reason);

        try {
            return refundRepository.save(payment.id(), payment.amount(), reason, OffsetDateTime.now(clock));
        } catch (DataIntegrityViolationException e) {
            throw new BusinessException(ErrorCode.PAYMENT_REFUND_ALREADY_EXISTS, "이미 진행 중인 환불 신청이 있습니다.");
        }
    }

    private Refund saveAsRequested(Payment payment, String reason) {
        try {
            return refundRepository.saveAsRequested(payment.id(), payment.amount(), reason);
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
