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
import java.util.Optional;
import com.coderhan.lastmission.payment.domain.Payment;
import com.coderhan.lastmission.payment.domain.PaymentStatus;
import com.coderhan.lastmission.payment.domain.Refund;
import com.coderhan.lastmission.payment.domain.RefundStatus;
import com.coderhan.lastmission.shared.error.BusinessException;
import com.coderhan.lastmission.shared.error.ErrorCode;
import com.coderhan.lastmission.shared.order.OrderType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
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
    private static final BigDecimal AMOUNT = BigDecimal.valueOf(10000);
    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-07-23T10:00:00Z");
    private static final LocalDate TODAY = NOW.toLocalDate();

    @Mock RefundRepository refundRepository;
    @Mock PaymentRepository paymentRepository;
    @Mock PaymentGateway paymentGateway;
    @Mock EventScheduleReader eventScheduleReader;
    @Mock PaymentEventRecorder eventRecorder;
    @Spy Clock clock = Clock.fixed(Instant.parse("2026-07-23T10:00:00Z"), ZoneOffset.UTC);

    @InjectMocks RefundService service;

    /**
     * 정책의 핵심: 행사 시작까지 남은 일수에 따른 환불율 계산 + 계산된 금액이 그대로
     * 토스 결제취소 요청 금액으로 전달되는지. D-7 이상 100%, D-3~D-6 50%, D-1~D-2 30%.
     */
    @ParameterizedTest(name = "행사까지 D-{0}이면 환불액은 {1}원이다")
    @CsvSource({
        "7,  10000",
        "6,  5000",
        "3,  5000",
        "2,  3000",
        "1,  3000",
    })
    @DisplayName("행사 시작까지 남은 일수에 따라 구간별 정률로 자동 환불한다")
    void appliesTieredRefundRateBasedOnDaysUntilEventStart(long daysUntilStart, long expectedAmountValue) {
        Payment payment = completedPayment();
        BigDecimal expectedAmount = BigDecimal.valueOf(expectedAmountValue);
        Refund saved = savedRefund(expectedAmount);
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(payment));
        when(refundRepository.findActiveByPaymentId(PAYMENT_ID)).thenReturn(Optional.empty());
        when(eventScheduleReader.findEventStartDate(anyString())).thenReturn(TODAY.plusDays(daysUntilStart));
        when(eventRecorder.reportRefund(anyLong(), anyString(), any(), any(), anyString(), any())).thenReturn(saved);

        Refund result = service.request(USER_ID, PAYMENT_ID, "단순 변심");

        assertThat(result).isSameAs(saved);
        verify(eventRecorder).reportRefund(PAYMENT_ID, payment.orderId(), payment.orderType(), expectedAmount,
                "단순 변심", OffsetDateTime.now(clock));
        verify(paymentGateway).cancel(payment.pgTransactionId(), "단순 변심", expectedAmount);
    }

    /**
     * 정책 변경: 행사 시작일(D-0) 이후는 환불율 0%로 조용히 접수하는 대신 신청 자체를 막는다.
     * 0원 환불을 접수하면 결제가 REFUNDED로 바뀌면서 정산 집계에서 통째로 빠지는 문제가 있었다.
     */
    @ParameterizedTest(name = "행사까지 D-{0}이면 환불 신청을 거부한다")
    @ValueSource(longs = {0, -1, -30})
    @DisplayName("행사 시작일이거나 이미 지난 결제는 환불 신청을 거부한다")
    void rejectsRefundRequestWhenEventAlreadyStartedOrPast(long daysUntilStart) {
        Payment payment = completedPayment();
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(payment));
        when(refundRepository.findActiveByPaymentId(PAYMENT_ID)).thenReturn(Optional.empty());
        when(eventScheduleReader.findEventStartDate(anyString())).thenReturn(TODAY.plusDays(daysUntilStart));

        assertThatThrownBy(() -> service.request(USER_ID, PAYMENT_ID, "단순 변심"))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(ErrorCode.PAYMENT_REFUND_NOT_ALLOWED));

        verify(paymentGateway, never()).cancel(any(), any(), any());
        verify(eventRecorder, never()).reportRefund(anyLong(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("광고 결제는 행사 시작일과 무관하게 항상 전액(100%) 환불한다")
    void refundsAdvertisementPaymentInFullRegardlessOfEventSchedule() {
        Payment payment = adPayment();
        Refund saved = savedRefund(AMOUNT);
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(payment));
        when(refundRepository.findActiveByPaymentId(PAYMENT_ID)).thenReturn(Optional.empty());
        when(eventRecorder.reportRefund(anyLong(), anyString(), any(), any(), anyString(), any())).thenReturn(saved);

        Refund result = service.request(USER_ID, PAYMENT_ID, "광고 취소");

        assertThat(result).isSameAs(saved);
        verify(eventRecorder).reportRefund(PAYMENT_ID, payment.orderId(), payment.orderType(), AMOUNT,
                "광고 취소", OffsetDateTime.now(clock));
        verify(paymentGateway).cancel(payment.pgTransactionId(), "광고 취소", AMOUNT);
        verify(eventScheduleReader, never()).findEventStartDate(any());
    }

    @Test
    @DisplayName("결제 내역이 없으면 PAYMENT_NOT_FOUND 예외를 던지고 아무것도 저장하지 않는다")
    void rejectsWhenPaymentNotFound() {
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.request(USER_ID, PAYMENT_ID, "사유"))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.PAYMENT_NOT_FOUND));

        verify(eventRecorder, never()).reportRefund(anyLong(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("본인 결제가 아니면 PAYMENT_ACCESS_DENIED 예외를 던지고 아무것도 저장하지 않는다")
    void rejectsWhenPaymentBelongsToAnotherUser() {
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(completedPayment()));

        assertThatThrownBy(() -> service.request(OTHER_USER_ID, PAYMENT_ID, "사유"))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.errorCode()).isEqualTo(ErrorCode.PAYMENT_ACCESS_DENIED));

        verify(eventRecorder, never()).reportRefund(anyLong(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("결제가 COMPLETED 상태가 아니면 PAYMENT_REFUND_NOT_ALLOWED 예외를 던진다")
    void rejectsWhenPaymentNotCompleted() {
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(failedPayment()));

        assertThatThrownBy(() -> service.request(USER_ID, PAYMENT_ID, "사유"))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(ErrorCode.PAYMENT_REFUND_NOT_ALLOWED));

        verify(eventRecorder, never()).reportRefund(anyLong(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("이미 진행 중인 환불 신청이 있으면 PAYMENT_REFUND_ALREADY_EXISTS 예외를 던진다")
    void rejectsWhenActiveRequestAlreadyExists() {
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(completedPayment()));
        when(refundRepository.findActiveByPaymentId(PAYMENT_ID)).thenReturn(Optional.of(savedRefund(AMOUNT)));

        assertThatThrownBy(() -> service.request(USER_ID, PAYMENT_ID, "사유"))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(ErrorCode.PAYMENT_REFUND_ALREADY_EXISTS));

        verify(eventRecorder, never()).reportRefund(anyLong(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("저장 시점에 유니크 제약 위반(동시 신청 경합)이 나면 PAYMENT_REFUND_ALREADY_EXISTS로 변환한다")
    void translatesConcurrentDuplicateSaveIntoBusinessException() {
        when(paymentRepository.findById(PAYMENT_ID)).thenReturn(Optional.of(completedPayment()));
        when(refundRepository.findActiveByPaymentId(PAYMENT_ID)).thenReturn(Optional.empty());
        when(eventScheduleReader.findEventStartDate(anyString())).thenReturn(TODAY.plusDays(7));
        when(eventRecorder.reportRefund(anyLong(), anyString(), any(), any(), anyString(), any()))
                .thenThrow(new DataIntegrityViolationException("duplicate"));

        assertThatThrownBy(() -> service.request(USER_ID, PAYMENT_ID, "사유"))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(ErrorCode.PAYMENT_REFUND_ALREADY_EXISTS));
    }

    private static Payment completedPayment() {
        return Payment.builder()
                .id(PAYMENT_ID)
                .orderId("ORD-1")
                .orderType(OrderType.RESERVATION)
                .userId(USER_ID)
                .idempotencyKey("key-1")
                .amount(AMOUNT)
                .method("CARD")
                .status(PaymentStatus.COMPLETED)
                .pgProvider("TOSS")
                .pgTransactionId("pg-tx-1")
                .build();
    }

    private static Payment adPayment() {
        return Payment.builder()
                .id(PAYMENT_ID)
                .orderId("AD-ORD-1")
                .orderType(OrderType.ADVERTISEMENT)
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

    private static Refund savedRefund(BigDecimal amount) {
        return Refund.builder()
                .id(1L)
                .paymentId(PAYMENT_ID)
                .amount(amount)
                .reason("단순 변심")
                .status(RefundStatus.COMPLETED)
                .autoApproved(true)
                .requestedAt(NOW)
                .refundedAt(NOW)
                .createdAt(NOW)
                .updatedAt(NOW)
                .build();
    }
}
