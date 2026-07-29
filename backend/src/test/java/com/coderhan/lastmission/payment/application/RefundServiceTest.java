package com.coderhan.lastmission.payment.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import com.coderhan.lastmission.payment.domain.Payment;
import com.coderhan.lastmission.payment.domain.PaymentStatus;
import com.coderhan.lastmission.payment.domain.Refund;
import com.coderhan.lastmission.payment.domain.RefundStatus;
import com.coderhan.lastmission.shared.error.BusinessException;
import com.coderhan.lastmission.shared.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class RefundServiceTest {
    private static final long USER_ID = 7L;
    private static final long OTHER_USER_ID = 99L;
    private static final long PAYMENT_ID = 5L;
    private static final BigDecimal AMOUNT = BigDecimal.valueOf(10000);
    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-07-23T10:00:00Z");

    @Mock RefundRepository refundRepository;
    @Mock PaymentRepository paymentRepository;

    @InjectMocks RefundService service;

    @Test
    void requestsRequestForCompletedPaymentOwnedByUser() {
        Payment payment = completedPayment();
        Refund saved = request(RefundStatus.REQUESTED);
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(payment));
        when(refundRepository.findActiveByPaymentId(PAYMENT_ID)).thenReturn(Optional.empty());
        when(refundRepository.save(anyLong(), any(), any())).thenReturn(saved);

        Refund result = service.request(USER_ID, PAYMENT_ID, "단순 변심");

        assertThat(result).isSameAs(saved);
        verify(refundRepository).save(PAYMENT_ID, AMOUNT, "단순 변심");
    }

    @Test
    void rejectsWhenPaymentNotFound() {
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.request(USER_ID, PAYMENT_ID, "사유"))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.PAYMENT_NOT_FOUND));

        verify(refundRepository, never()).save(anyLong(), any(), any());
    }

    @Test
    void rejectsWhenPaymentBelongsToAnotherUser() {
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(completedPayment()));

        assertThatThrownBy(() -> service.request(OTHER_USER_ID, PAYMENT_ID, "사유"))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.PAYMENT_ACCESS_DENIED));

        verify(refundRepository, never()).save(anyLong(), any(), any());
    }

    @Test
    void rejectsWhenPaymentNotCompleted() {
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(failedPayment()));

        assertThatThrownBy(() -> service.request(USER_ID, PAYMENT_ID, "사유"))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(ErrorCode.PAYMENT_REFUND_NOT_ALLOWED));

        verify(refundRepository, never()).save(anyLong(), any(), any());
    }

    @Test
    void rejectsWhenActiveRequestAlreadyExists() {
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(completedPayment()));
        when(refundRepository.findActiveByPaymentId(PAYMENT_ID))
                .thenReturn(Optional.of(request(RefundStatus.REQUESTED)));

        assertThatThrownBy(() -> service.request(USER_ID, PAYMENT_ID, "사유"))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(ErrorCode.PAYMENT_REFUND_ALREADY_EXISTS));

        verify(refundRepository, never()).save(anyLong(), any(), any());
    }

    @Test
    void translatesConcurrentDuplicateSaveIntoBusinessException() {
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(completedPayment()));
        when(refundRepository.findActiveByPaymentId(PAYMENT_ID)).thenReturn(Optional.empty());
        when(refundRepository.save(anyLong(), any(), any()))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThatThrownBy(() -> service.request(USER_ID, PAYMENT_ID, "사유"))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(ErrorCode.PAYMENT_REFUND_ALREADY_EXISTS));
    }

    private static Payment completedPayment() {
        return Payment.builder()
                .id(PAYMENT_ID)
                .orderId("ORD-1")
                .userId(USER_ID)
                .idempotencyKey("key-1")
                .amount(AMOUNT)
                .method("CARD")
                .status(PaymentStatus.COMPLETED)
                .pgProvider("TOSS")
                .build();
    }

    private static Payment failedPayment() {
        return Payment.builder()
                .id(PAYMENT_ID)
                .orderId("ORD-1")
                .userId(USER_ID)
                .idempotencyKey("key-1")
                .amount(AMOUNT)
                .method("CARD")
                .status(PaymentStatus.FAILED)
                .pgProvider("TOSS")
                .build();
    }

    private static Refund request(RefundStatus status) {
        return Refund.builder()
                .id(1L)
                .paymentId(PAYMENT_ID)
                .amount(AMOUNT)
                .reason("단순 변심")
                .status(status)
                .autoApproved(false)
                .requestedAt(NOW)
                .createdAt(NOW)
                .updatedAt(NOW)
                .build();
    }
}
