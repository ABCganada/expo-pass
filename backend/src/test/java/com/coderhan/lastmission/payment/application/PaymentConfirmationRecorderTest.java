package com.coderhan.lastmission.payment.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import com.coderhan.lastmission.payment.domain.Payment;
import com.coderhan.lastmission.payment.domain.PaymentStatus;
import com.coderhan.lastmission.shared.event.PaymentConfirmedEvent;
import com.coderhan.lastmission.shared.order.OrderType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class PaymentConfirmationRecorderTest {
    private static final long PAYMENT_ID = 5L;
    private static final String ORDER_ID = "ORD-1";
    private static final OrderType ORDER_TYPE = OrderType.RESERVATION;
    private static final long USER_ID = 7L;
    private static final String PAYMENT_KEY = "payment-key-1";
    private static final BigDecimal AMOUNT = BigDecimal.valueOf(10000);
    private static final String PG_ORDER_ID = "pg-order-1";
    private static final OffsetDateTime APPROVED_AT = OffsetDateTime.parse("2026-07-23T10:00:00Z");

    @Mock PaymentRepository repository;
    @Mock ApplicationEventPublisher eventPublisher;

    @InjectMocks PaymentConfirmationRecorder recorder;

    @Test
    @DisplayName("저장에 성공하면 저장된 결제 정보를 담아 PaymentConfirmedEvent를 발행한다")
    void publishesPaymentConfirmedEventAfterSave() {
        Payment saved = Payment.builder()
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
        when(repository.save(ORDER_ID, ORDER_TYPE, USER_ID, PAYMENT_KEY, AMOUNT, "CARD", "TOSS",
                PG_ORDER_ID, PAYMENT_KEY, APPROVED_AT)).thenReturn(saved);

        Payment result = recorder.save(ORDER_ID, ORDER_TYPE, USER_ID, PAYMENT_KEY, AMOUNT, "CARD", "TOSS",
                PG_ORDER_ID, PAYMENT_KEY, APPROVED_AT);

        assertThat(result).isSameAs(saved);

        ArgumentCaptor<PaymentConfirmedEvent> captor = ArgumentCaptor.forClass(PaymentConfirmedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        PaymentConfirmedEvent event = captor.getValue();
        assertThat(event.paymentId()).isEqualTo(PAYMENT_ID);
        assertThat(event.orderId()).isEqualTo(ORDER_ID);
        assertThat(event.orderType()).isEqualTo(ORDER_TYPE);
        assertThat(event.confirmedAt()).isEqualTo(APPROVED_AT);
    }
}
