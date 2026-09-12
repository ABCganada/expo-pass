package com.coderhan.lastmission.payment.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import com.coderhan.lastmission.payment.domain.Payment;
import com.coderhan.lastmission.payment.domain.PaymentStatus;
import com.coderhan.lastmission.payment.domain.Refund;
import com.coderhan.lastmission.payment.domain.RefundStatus;
import com.coderhan.lastmission.shared.event.PaymentConfirmedEvent;
import com.coderhan.lastmission.shared.event.PaymentFailedEvent;
import com.coderhan.lastmission.shared.event.PaymentRefundedEvent;
import com.coderhan.lastmission.shared.order.OrderType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class PaymentEventRecorderTest {
    private static final long PAYMENT_ID = 5L;
    private static final String ORDER_ID = "ORD-1";
    private static final OrderType ORDER_TYPE = OrderType.RESERVATION;
    private static final long USER_ID = 7L;
    private static final String PAYMENT_KEY = "payment-key-1";
    private static final BigDecimal AMOUNT = BigDecimal.valueOf(10000);
    private static final String PG_ORDER_ID = "pg-order-1";
    private static final OffsetDateTime APPROVED_AT = OffsetDateTime.parse("2026-07-23T10:00:00Z");

    @Mock PaymentRepository repository;
    @Mock PaymentLogRepository paymentLogRepository;
    @Mock RefundRepository refundRepository;
    @Mock ApplicationEventPublisher eventPublisher;
    @Spy ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks PaymentEventRecorder recorder;

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

        Payment result = recorder.reportConfirmation(ORDER_ID, ORDER_TYPE, USER_ID, PAYMENT_KEY, AMOUNT, "CARD", "TOSS",
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

    @Test
    @DisplayName("reportFailure()를 호출하면 FAILED 감사 로그를 payment_logs에 저장하고 PaymentFailedEvent를 발행한다")
    void savesFailedAuditLogAndPublishesPaymentFailedEvent() {
        recorder.reportFailure(ORDER_ID, ORDER_TYPE, USER_ID, AMOUNT, "사용자가 결제창에서 취소함", APPROVED_AT);

        ArgumentCaptor<String> payloadCaptor = ArgumentCaptor.forClass(String.class);
        verify(paymentLogRepository).save(isNull(), eq("PAYMENT_FAILED"), payloadCaptor.capture(),
                isNull(), isNull(), eq(APPROVED_AT));
        assertThat(payloadCaptor.getValue())
                .contains("\"orderId\":\"" + ORDER_ID + "\"")
                .contains("\"reason\":\"사용자가 결제창에서 취소함\"");

        ArgumentCaptor<PaymentFailedEvent> captor = ArgumentCaptor.forClass(PaymentFailedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        PaymentFailedEvent event = captor.getValue();
        assertThat(event.orderId()).isEqualTo(ORDER_ID);
        assertThat(event.orderType()).isEqualTo(ORDER_TYPE);
        assertThat(event.userId()).isEqualTo(USER_ID);
        assertThat(event.reason()).isEqualTo("사용자가 결제창에서 취소함");
        assertThat(event.failedAt()).isEqualTo(APPROVED_AT);
    }

    @Test
    @DisplayName("reportRefund()를 호출하면 payments.status를 REFUNDED로 갱신하고 PaymentRefundedEvent를 발행한다")
    void reportsRefundAndPublishesPaymentRefundedEvent() {
        Refund saved = Refund.builder()
                .id(1L)
                .paymentId(PAYMENT_ID)
                .amount(AMOUNT)
                .reason("단순 변심")
                .status(RefundStatus.COMPLETED)
                .autoApproved(true)
                .requestedAt(APPROVED_AT)
                .refundedAt(APPROVED_AT)
                .createdAt(APPROVED_AT)
                .updatedAt(APPROVED_AT)
                .build();
        when(refundRepository.save(PAYMENT_ID, AMOUNT, "단순 변심", APPROVED_AT)).thenReturn(saved);

        Refund result = recorder.reportRefund(PAYMENT_ID, ORDER_ID, ORDER_TYPE, AMOUNT, "단순 변심", APPROVED_AT);

        assertThat(result).isSameAs(saved);
        verify(repository).markRefunded(PAYMENT_ID, APPROVED_AT);

        ArgumentCaptor<PaymentRefundedEvent> captor = ArgumentCaptor.forClass(PaymentRefundedEvent.class);
        verify(eventPublisher).publishEvent(captor.capture());
        PaymentRefundedEvent event = captor.getValue();
        assertThat(event.orderId()).isEqualTo(ORDER_ID);
        assertThat(event.orderType()).isEqualTo(ORDER_TYPE);
        assertThat(event.paymentId()).isEqualTo(PAYMENT_ID);
        assertThat(event.refundAmount()).isEqualTo(AMOUNT);
        assertThat(event.refundedAt()).isEqualTo(APPROVED_AT);
    }
}
