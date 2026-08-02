package com.coderhan.lastmission.payment.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.Optional;
import com.coderhan.lastmission.marketing.BannerOrderDirectory;
import com.coderhan.lastmission.payment.domain.OrderType;
import com.coderhan.lastmission.payment.domain.Payment;
import com.coderhan.lastmission.payment.domain.PaymentStatus;
import com.coderhan.lastmission.reservation.ReservationOrderDirectory;
import com.coderhan.lastmission.shared.error.BusinessException;
import com.coderhan.lastmission.shared.error.ErrorCode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {
    private static final long USER_ID = 7L;
    private static final long OTHER_USER_ID = 99L;
    private static final long PAYMENT_ID = 5L;
    private static final String ORDER_ID = "ORD-1";
    private static final OrderType ORDER_TYPE = OrderType.RESERVATION;
    private static final String PG_ORDER_ID = "pg-order-1";
    private static final String PAYMENT_KEY = "payment-key-1";
    private static final BigDecimal AMOUNT = BigDecimal.valueOf(10000);
    private static final OffsetDateTime APPROVED_AT = OffsetDateTime.parse("2026-07-23T10:00:00Z");

    @Mock PaymentRepository repository;
    @Mock PaymentGateway paymentGateway;
    @Mock ReservationOrderDirectory reservationOrderDirectory;
    @Mock BannerOrderDirectory bannerOrderDirectory;

    @InjectMocks PaymentService service;

    @Test
    @DisplayName("동일한 paymentKey로 이미 승인된 결제가 있으면 토스를 다시 호출하지 않고 기존 결제를 반환한다")
    void returnsExistingPaymentWithoutCallingGatewayWhenIdempotencyKeyAlreadyProcessed() {
        Payment existing = completedPayment();
        when(reservationOrderDirectory.findOrderAmount(ORDER_ID)).thenReturn(Optional.of(AMOUNT));
        when(repository.findByIdempotencyKey(PAYMENT_KEY)).thenReturn(Optional.of(existing));

        Payment result = service.confirm(USER_ID, ORDER_ID, ORDER_TYPE, PG_ORDER_ID, PAYMENT_KEY, AMOUNT);

        assertThat(result).isSameAs(existing);
        verify(paymentGateway, never()).confirm(any(), any(), any());
        verify(repository, never()).save(any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("신규 결제면 토스 승인을 호출하고 그 결과를 그대로 저장한다")
    void confirmsWithGatewayAndSavesResultForNewPayment() {
        Payment saved = completedPayment();
        when(reservationOrderDirectory.findOrderAmount(ORDER_ID)).thenReturn(Optional.of(AMOUNT));
        when(repository.findByIdempotencyKey(PAYMENT_KEY)).thenReturn(Optional.empty());
        when(paymentGateway.confirm(PAYMENT_KEY, PG_ORDER_ID, AMOUNT))
                .thenReturn(new PaymentGateway.ConfirmResult("CARD", APPROVED_AT));
        when(repository.save(ORDER_ID, ORDER_TYPE, USER_ID, PAYMENT_KEY, AMOUNT, "CARD", "TOSS",
                PG_ORDER_ID, PAYMENT_KEY, APPROVED_AT)).thenReturn(saved);

        Payment result = service.confirm(USER_ID, ORDER_ID, ORDER_TYPE, PG_ORDER_ID, PAYMENT_KEY, AMOUNT);

        assertThat(result).isSameAs(saved);
    }

    @Test
    @DisplayName("orderType이 ADVERTISEMENT면 BannerOrderDirectory로 금액을 검증하고, ReservationOrderDirectory는 호출하지 않는다")
    void confirmsAdvertisementOrderUsingBannerOrderDirectory() {
        Payment saved = completedPayment();
        when(bannerOrderDirectory.findOrderAmount(ORDER_ID)).thenReturn(Optional.of(AMOUNT));
        when(repository.findByIdempotencyKey(PAYMENT_KEY)).thenReturn(Optional.empty());
        when(paymentGateway.confirm(PAYMENT_KEY, PG_ORDER_ID, AMOUNT))
                .thenReturn(new PaymentGateway.ConfirmResult("CARD", APPROVED_AT));
        when(repository.save(ORDER_ID, OrderType.ADVERTISEMENT, USER_ID, PAYMENT_KEY, AMOUNT, "CARD", "TOSS",
                PG_ORDER_ID, PAYMENT_KEY, APPROVED_AT)).thenReturn(saved);

        Payment result = service.confirm(USER_ID, ORDER_ID, OrderType.ADVERTISEMENT, PG_ORDER_ID, PAYMENT_KEY, AMOUNT);

        assertThat(result).isSameAs(saved);
        verify(reservationOrderDirectory, never()).findOrderAmount(any());
    }

    @Test
    @DisplayName("저장 시점에 idempotency_key 경합이 나면 먼저 커밋된 결제를 반환한다(토스 이중 승인 방지)")
    void returnsAlreadyCommittedPaymentWhenSaveRacesOnIdempotencyKey() {
        Payment committedByOtherRequest = completedPayment();
        when(reservationOrderDirectory.findOrderAmount(ORDER_ID)).thenReturn(Optional.of(AMOUNT));
        when(repository.findByIdempotencyKey(PAYMENT_KEY))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(committedByOtherRequest));
        when(paymentGateway.confirm(PAYMENT_KEY, PG_ORDER_ID, AMOUNT))
                .thenReturn(new PaymentGateway.ConfirmResult("CARD", APPROVED_AT));
        when(repository.save(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(new DataIntegrityViolationException("duplicate idempotency_key"));

        Payment result = service.confirm(USER_ID, ORDER_ID, ORDER_TYPE, PG_ORDER_ID, PAYMENT_KEY, AMOUNT);

        assertThat(result).isSameAs(committedByOtherRequest);
    }

    @Test
    @DisplayName("저장 실패가 idempotency_key 경합이 아니면(order_id 중복 등) 원래 예외를 그대로 던진다")
    void rethrowsOriginalExceptionWhenSaveFailsForReasonOtherThanIdempotencyKeyRace() {
        when(reservationOrderDirectory.findOrderAmount(ORDER_ID)).thenReturn(Optional.of(AMOUNT));
        when(repository.findByIdempotencyKey(PAYMENT_KEY)).thenReturn(Optional.empty());
        when(paymentGateway.confirm(PAYMENT_KEY, PG_ORDER_ID, AMOUNT))
                .thenReturn(new PaymentGateway.ConfirmResult("CARD", APPROVED_AT));
        DataIntegrityViolationException saveFailure = new DataIntegrityViolationException("duplicate order_id");
        when(repository.save(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenThrow(saveFailure);

        assertThatThrownBy(() -> service.confirm(USER_ID, ORDER_ID, ORDER_TYPE, PG_ORDER_ID, PAYMENT_KEY, AMOUNT))
                .isSameAs(saveFailure);
    }

    @Test
    @DisplayName("결제 금액이 올바르지 않으면 주문 조회 전에 PAYMENT_INVALID_REQUEST 예외를 던진다")
    void rejectsInvalidAmountBeforeCallingGateway() {
        assertThatThrownBy(() -> service.confirm(USER_ID, ORDER_ID, ORDER_TYPE, PG_ORDER_ID, PAYMENT_KEY, BigDecimal.ZERO))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(ErrorCode.PAYMENT_INVALID_REQUEST));

        verify(reservationOrderDirectory, never()).findOrderAmount(any());
        verify(paymentGateway, never()).confirm(any(), any(), any());
    }

    @Test
    @DisplayName("결제 금액이 주문 금액과 다르면 게이트웨이를 호출하기 전에 PAYMENT_INVALID_REQUEST 예외를 던진다")
    void rejectsAmountMismatchWithOrderBeforeCallingGateway() {
        when(reservationOrderDirectory.findOrderAmount(ORDER_ID)).thenReturn(Optional.of(BigDecimal.valueOf(9000)));

        assertThatThrownBy(() -> service.confirm(USER_ID, ORDER_ID, ORDER_TYPE, PG_ORDER_ID, PAYMENT_KEY, AMOUNT))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(ErrorCode.PAYMENT_INVALID_REQUEST));

        verify(paymentGateway, never()).confirm(any(), any(), any());
    }

    @Test
    @DisplayName("주문 내역을 찾을 수 없으면 게이트웨이를 호출하기 전에 PAYMENT_INVALID_REQUEST 예외를 던진다")
    void rejectsWhenOrderNotFound() {
        when(reservationOrderDirectory.findOrderAmount(ORDER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.confirm(USER_ID, ORDER_ID, ORDER_TYPE, PG_ORDER_ID, PAYMENT_KEY, AMOUNT))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(ErrorCode.PAYMENT_INVALID_REQUEST));

        verify(paymentGateway, never()).confirm(any(), any(), any());
    }

    @Test
    @DisplayName("광고 주문의 결제 금액이 주문 금액과 다르면 BannerOrderDirectory 기준으로 PAYMENT_INVALID_REQUEST 예외를 던진다")
    void rejectsAdvertisementAmountMismatchWithOrderBeforeCallingGateway() {
        when(bannerOrderDirectory.findOrderAmount(ORDER_ID)).thenReturn(Optional.of(BigDecimal.valueOf(9000)));

        assertThatThrownBy(() -> service.confirm(USER_ID, ORDER_ID, OrderType.ADVERTISEMENT, PG_ORDER_ID,
                PAYMENT_KEY, AMOUNT))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(ErrorCode.PAYMENT_INVALID_REQUEST));

        verify(reservationOrderDirectory, never()).findOrderAmount(any());
        verify(paymentGateway, never()).confirm(any(), any(), any());
    }

    @Test
    @DisplayName("광고 주문 내역을 찾을 수 없으면 BannerOrderDirectory 기준으로 PAYMENT_INVALID_REQUEST 예외를 던진다")
    void rejectsWhenAdvertisementOrderNotFound() {
        when(bannerOrderDirectory.findOrderAmount(ORDER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.confirm(USER_ID, ORDER_ID, OrderType.ADVERTISEMENT, PG_ORDER_ID,
                PAYMENT_KEY, AMOUNT))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(ErrorCode.PAYMENT_INVALID_REQUEST));

        verify(reservationOrderDirectory, never()).findOrderAmount(any());
        verify(paymentGateway, never()).confirm(any(), any(), any());
    }

    @Test
    @DisplayName("본인 결제가 아니면 PAYMENT_ACCESS_DENIED 예외를 던진다")
    void rejectsGetPaymentForAnotherUsersPayment() {
        when(repository.findById(PAYMENT_ID)).thenReturn(Optional.of(completedPayment()));

        assertThatThrownBy(() -> service.getPayment(OTHER_USER_ID, PAYMENT_ID))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(ErrorCode.PAYMENT_ACCESS_DENIED));
    }

    @Test
    @DisplayName("결제 내역이 없으면 PAYMENT_NOT_FOUND 예외를 던진다")
    void rejectsGetPaymentWhenNotFound() {
        when(repository.findById(PAYMENT_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getPayment(USER_ID, PAYMENT_ID))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(ErrorCode.PAYMENT_NOT_FOUND));
    }

    private static Payment completedPayment() {
        return Payment.builder()
                .id(PAYMENT_ID)
                .orderId(ORDER_ID)
                .orderType(ORDER_TYPE)
                .userId(USER_ID)
                .idempotencyKey(PAYMENT_KEY)
                .amount(AMOUNT)
                .method("CARD")
                .status(PaymentStatus.COMPLETED)
                .pgProvider("TOSS")
                .pgOrderId(PG_ORDER_ID)
                .pgTransactionId(PAYMENT_KEY)
                .paidAt(APPROVED_AT)
                .build();
    }
}
