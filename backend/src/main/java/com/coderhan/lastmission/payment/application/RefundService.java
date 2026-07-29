package com.coderhan.lastmission.payment.application;

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

    private final RefundRepository refundRepository;
    private final PaymentRepository paymentRepository;

    /**
     * 환불 신청 접수. 환불 금액은 항상 결제 전액
     * 자동승인/토스 결제취소는 이 메서드의 책임이 아니다 — 여기서는 REQUESTED 상태로
     * 접수만 하고, 실제 승인·취소는 별도의 관리자 승인 플로우에서 처리한다.
     */
    public Refund request(long userId, long paymentId, String reason) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.PAYMENT_NOT_FOUND, "결제 내역을 찾을 수 없습니다."));

        validateOwner(userId, payment);
        validateRefundable(payment);
        validateNoActiveRefund(paymentId);

        try {
            return refundRepository.save(paymentId, payment.amount(), reason);
        } catch (DataIntegrityViolationException e) {
            /** idx_payment_refunds_unique_active 위반 — 검증과 저장 사이의 경합으로 먼저
             * 접수된 신청이 있는 경우다. */
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
