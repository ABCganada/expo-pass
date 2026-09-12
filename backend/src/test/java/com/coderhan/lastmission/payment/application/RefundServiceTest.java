package com.coderhan.lastmission.payment.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import com.coderhan.lastmission.payment.domain.Payment;
import com.coderhan.lastmission.payment.domain.PaymentStatus;
import com.coderhan.lastmission.payment.domain.Refund;
import com.coderhan.lastmission.payment.domain.RefundStatus;
import com.coderhan.lastmission.shared.error.BusinessException;
import com.coderhan.lastmission.shared.error.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class RefundServiceTest {
    private static final long USER_ID = 7L;
    private static final long OTHER_USER_ID = 99L;
    private static final long PAYMENT_ID = 5L;
    private static final long OTHER_PAYMENT_ID = 6L;
    private static final long REFUND_ID = 1L;
    private static final BigDecimal AMOUNT = BigDecimal.valueOf(10000);
    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-07-23T10:00:00Z");
    private static final LocalDate TODAY = NOW.toLocalDate();

    @Mock RefundRepository refundRepository;
    @Mock PaymentRepository paymentRepository;
    @Mock PaymentGateway paymentGateway;
    @Mock EventScheduleReader eventScheduleReader;
    @Mock EventManagerLookup eventManagerLookup;
    @Spy Clock clock = Clock.fixed(Instant.parse("2026-07-23T10:00:00Z"), ZoneOffset.UTC);

    @InjectMocks RefundService service;

    @Test
    @DisplayName("행사 시작 3일 이내 신청이면 REQUESTED로만 저장하고 토스 취소는 호출하지 않는다")
    void fallsBackToManualApprovalWhenWithinThreeDaysOfEvent() {
        Payment payment = completedPayment();
        Refund saved = request(RefundStatus.REQUESTED);
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(payment));
        when(refundRepository.findActiveByPaymentId(PAYMENT_ID)).thenReturn(Optional.empty());
        when(eventScheduleReader.findEventStartDate(anyString())).thenReturn(TODAY.plusDays(2));
        when(refundRepository.saveAsRequested(anyLong(), any(), any())).thenReturn(saved);

        Refund result = service.request(USER_ID, PAYMENT_ID, "단순 변심");

        assertThat(result).isSameAs(saved);
        verify(refundRepository).saveAsRequested(PAYMENT_ID, AMOUNT, "단순 변심");
        verify(paymentGateway, never()).cancel(any(), any());
    }

    @Test
    @DisplayName("행사 시작 3일 이상 남았으면 토스 결제취소를 호출하고 COMPLETED로 저장한다")
    void autoApprovesAndCancelsPaymentWhenAtLeastThreeDaysBeforeEvent() {
        Payment payment = completedPayment();
        Refund saved = request(RefundStatus.COMPLETED);
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(payment));
        when(refundRepository.findActiveByPaymentId(PAYMENT_ID)).thenReturn(Optional.empty());
        when(eventScheduleReader.findEventStartDate(anyString())).thenReturn(TODAY.plusDays(3));
        when(refundRepository.save(anyLong(), any(), any(), any())).thenReturn(saved);

        Refund result = service.request(USER_ID, PAYMENT_ID, "단순 변심");

        assertThat(result).isSameAs(saved);
        verify(paymentGateway).cancel(payment.pgTransactionId(), "단순 변심");
        verify(refundRepository).save(PAYMENT_ID, AMOUNT, "단순 변심", OffsetDateTime.now(clock));
        verify(refundRepository, never()).saveAsRequested(anyLong(), any(), any());
    }

    @Test
    @DisplayName("결제 내역이 없으면 PAYMENT_NOT_FOUND 예외를 던지고 아무것도 저장하지 않는다")
    void rejectsWhenPaymentNotFound() {
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.request(USER_ID, PAYMENT_ID, "사유"))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.PAYMENT_NOT_FOUND));

        verify(refundRepository, never()).saveAsRequested(anyLong(), any(), any());
        verify(refundRepository, never()).save(anyLong(), any(), any(), any());
    }

    @Test
    @DisplayName("본인 결제가 아니면 PAYMENT_ACCESS_DENIED 예외를 던지고 아무것도 저장하지 않는다")
    void rejectsWhenPaymentBelongsToAnotherUser() {
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(completedPayment()));

        assertThatThrownBy(() -> service.request(OTHER_USER_ID, PAYMENT_ID, "사유"))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.PAYMENT_ACCESS_DENIED));

        verify(refundRepository, never()).saveAsRequested(anyLong(), any(), any());
        verify(refundRepository, never()).save(anyLong(), any(), any(), any());
    }

    @Test
    @DisplayName("결제가 COMPLETED 상태가 아니면 PAYMENT_REFUND_NOT_ALLOWED 예외를 던진다")
    void rejectsWhenPaymentNotCompleted() {
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(failedPayment()));

        assertThatThrownBy(() -> service.request(USER_ID, PAYMENT_ID, "사유"))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(ErrorCode.PAYMENT_REFUND_NOT_ALLOWED));

        verify(refundRepository, never()).saveAsRequested(anyLong(), any(), any());
        verify(refundRepository, never()).save(anyLong(), any(), any(), any());
    }

    @Test
    @DisplayName("이미 진행 중인 환불 신청이 있으면 PAYMENT_REFUND_ALREADY_EXISTS 예외를 던진다")
    void rejectsWhenActiveRequestAlreadyExists() {
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(completedPayment()));
        when(refundRepository.findActiveByPaymentId(PAYMENT_ID))
                .thenReturn(Optional.of(request(RefundStatus.REQUESTED)));

        assertThatThrownBy(() -> service.request(USER_ID, PAYMENT_ID, "사유"))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(ErrorCode.PAYMENT_REFUND_ALREADY_EXISTS));

        verify(refundRepository, never()).saveAsRequested(anyLong(), any(), any());
        verify(refundRepository, never()).save(anyLong(), any(), any(), any());
    }

    @Test
    @DisplayName("저장 시점에 유니크 제약 위반(동시 신청 경합)이 나면 PAYMENT_REFUND_ALREADY_EXISTS로 변환한다")
    void translatesConcurrentDuplicateSaveIntoBusinessException() {
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(completedPayment()));
        when(refundRepository.findActiveByPaymentId(PAYMENT_ID)).thenReturn(Optional.empty());
        when(eventScheduleReader.findEventStartDate(anyString())).thenReturn(TODAY.plusDays(2));
        when(refundRepository.saveAsRequested(anyLong(), any(), any()))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThatThrownBy(() -> service.request(USER_ID, PAYMENT_ID, "사유"))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(ErrorCode.PAYMENT_REFUND_ALREADY_EXISTS));
    }

    @Test
    @DisplayName("본인이 담당하는 행사의 환불이면 승인 시 토스 취소를 호출하고 COMPLETED로 갱신한다")
    void approvesRequestedRefundAndCancelsPayment() {
        Refund pending = request(RefundStatus.REQUESTED);
        Refund approved = request(RefundStatus.COMPLETED);
        when(refundRepository.findById(REFUND_ID)).thenReturn(Optional.of(pending));
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(completedPayment()));
        when(eventManagerLookup.findEventManagerId("ORD-1")).thenReturn(USER_ID);
        when(refundRepository.approve(REFUND_ID, USER_ID, OffsetDateTime.now(clock))).thenReturn(approved);

        Refund result = service.approve(USER_ID, REFUND_ID);

        assertThat(result).isSameAs(approved);
        verify(paymentGateway).cancel("pg-tx-1", "단순 변심");
        verify(refundRepository).approve(REFUND_ID, USER_ID, OffsetDateTime.now(clock));
    }

    @Test
    @DisplayName("본인이 담당하지 않는 행사의 환불이면 승인 시 PAYMENT_REFUND_ACCESS_DENIED 예외를 던진다")
    void deniesApprovalWhenCallerDoesNotManageEvent() {
        when(refundRepository.findById(REFUND_ID)).thenReturn(Optional.of(request(RefundStatus.REQUESTED)));
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(completedPayment()));
        when(eventManagerLookup.findEventManagerId("ORD-1")).thenReturn(OTHER_USER_ID);

        assertThatThrownBy(() -> service.approve(USER_ID, REFUND_ID))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(ErrorCode.PAYMENT_REFUND_ACCESS_DENIED));

        verify(paymentGateway, never()).cancel(any(), any());
        verify(refundRepository, never()).approve(anyLong(), anyLong(), any());
    }

    @Test
    @DisplayName("환불 신청 내역이 없으면 PAYMENT_REFUND_NOT_FOUND 예외를 던진다")
    void rejectsApprovalWhenRefundNotFound() {
        when(refundRepository.findById(REFUND_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.approve(USER_ID, REFUND_ID))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(ErrorCode.PAYMENT_REFUND_NOT_FOUND));

        verify(paymentGateway, never()).cancel(any(), any());
    }

    @Test
    @DisplayName("이미 승인/거절된 환불이면 PAYMENT_REFUND_ALREADY_DECIDED 예외를 던진다")
    void rejectsApprovalWhenAlreadyDecided() {
        when(refundRepository.findById(REFUND_ID)).thenReturn(Optional.of(request(RefundStatus.COMPLETED)));

        assertThatThrownBy(() -> service.approve(USER_ID, REFUND_ID))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(ErrorCode.PAYMENT_REFUND_ALREADY_DECIDED));

        verify(paymentGateway, never()).cancel(any(), any());
    }

    @Test
    @DisplayName("본인이 담당하는 행사의 환불이면 거절 시 토스는 호출하지 않고 REJECTED로 갱신한다")
    void marksRefundAsRejectedWithoutCallingGateway() {
        Refund pending = request(RefundStatus.REQUESTED);
        Refund rejected = request(RefundStatus.REJECTED);
        when(refundRepository.findById(REFUND_ID)).thenReturn(Optional.of(pending));
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(completedPayment()));
        when(eventManagerLookup.findEventManagerId("ORD-1")).thenReturn(USER_ID);
        when(refundRepository.reject(REFUND_ID, USER_ID, OffsetDateTime.now(clock))).thenReturn(rejected);

        Refund result = service.reject(USER_ID, REFUND_ID);

        assertThat(result).isSameAs(rejected);
        verify(refundRepository).reject(REFUND_ID, USER_ID, OffsetDateTime.now(clock));
        verify(paymentGateway, never()).cancel(any(), any());
    }

    @Test
    @DisplayName("본인이 담당하지 않는 행사의 환불이면 거절 시에도 PAYMENT_REFUND_ACCESS_DENIED 예외를 던진다")
    void deniesRejectionWhenCallerDoesNotManageEvent() {
        when(refundRepository.findById(REFUND_ID)).thenReturn(Optional.of(request(RefundStatus.REQUESTED)));
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(completedPayment()));
        when(eventManagerLookup.findEventManagerId("ORD-1")).thenReturn(OTHER_USER_ID);

        assertThatThrownBy(() -> service.reject(USER_ID, REFUND_ID))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(ErrorCode.PAYMENT_REFUND_ACCESS_DENIED));

        verify(refundRepository, never()).reject(anyLong(), anyLong(), any());
    }

    @Test
    @DisplayName("환불 신청 내역이 없으면 거절 시에도 PAYMENT_REFUND_NOT_FOUND 예외를 던진다")
    void rejectionFailsWhenRefundNotFound() {
        when(refundRepository.findById(REFUND_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.reject(USER_ID, REFUND_ID))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(ErrorCode.PAYMENT_REFUND_NOT_FOUND));

        verify(refundRepository, never()).reject(anyLong(), anyLong(), any());
    }

    @Test
    @DisplayName("이미 승인/거절된 환불이면 거절 시에도 PAYMENT_REFUND_ALREADY_DECIDED 예외를 던진다")
    void rejectionFailsWhenAlreadyDecided() {
        when(refundRepository.findById(REFUND_ID)).thenReturn(Optional.of(request(RefundStatus.COMPLETED)));

        assertThatThrownBy(() -> service.reject(USER_ID, REFUND_ID))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(ErrorCode.PAYMENT_REFUND_ALREADY_DECIDED));

        verify(refundRepository, never()).reject(anyLong(), anyLong(), any());
    }

    @Test
    @DisplayName("본인이 담당하는 행사의 환불만 대기 목록에서 본다")
    void listsOnlyRefundsForOwnedEvent() {
        Refund owned = request(RefundStatus.REQUESTED);
        Refund notOwned = requestForPayment(RefundStatus.REQUESTED, OTHER_PAYMENT_ID);
        when(refundRepository.findAllRequested()).thenReturn(List.of(owned, notOwned));
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(completedPayment()));
        when(paymentRepository.findById(OTHER_PAYMENT_ID))
                .thenReturn(Optional.of(completedPaymentWithOrderId(OTHER_PAYMENT_ID, "ORD-2")));
        when(eventManagerLookup.findEventManagerId("ORD-1")).thenReturn(USER_ID);
        when(eventManagerLookup.findEventManagerId("ORD-2")).thenReturn(OTHER_USER_ID);

        List<Refund> result = service.listPending(USER_ID);

        assertThat(result).containsExactly(owned);
    }

    private static Payment completedPayment() {
        return completedPaymentWithOrderId(PAYMENT_ID, "ORD-1");
    }

    private static Payment completedPaymentWithOrderId(long paymentId, String orderId) {
        return Payment.builder()
                .id(paymentId)
                .orderId(orderId)
                .userId(USER_ID)
                .idempotencyKey("key-1")
                .amount(AMOUNT)
                .method("CARD")
                .status(PaymentStatus.COMPLETED)
                .pgProvider("TOSS")
                .pgTransactionId("pg-tx-1")
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
        return requestForPayment(status, PAYMENT_ID);
    }

    private static Refund requestForPayment(RefundStatus status, long paymentId) {
        return Refund.builder()
                .id(REFUND_ID)
                .paymentId(paymentId)
                .amount(AMOUNT)
                .reason("단순 변심")
                .status(status)
                .autoApproved(status == RefundStatus.COMPLETED)
                .requestedAt(NOW)
                .createdAt(NOW)
                .updatedAt(NOW)
                .build();
    }
}
