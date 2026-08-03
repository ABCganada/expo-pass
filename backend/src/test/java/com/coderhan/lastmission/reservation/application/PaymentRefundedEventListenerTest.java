package com.coderhan.lastmission.reservation.application;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import com.coderhan.lastmission.shared.event.PaymentRefundedEvent;
import com.coderhan.lastmission.shared.order.OrderType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentRefundedEventListenerTest {
    private static final String ORDER_ID = "ORD-20260723-000001";
    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-07-23T10:00:00Z");

    @Mock ReservationService reservationService;

    @InjectMocks PaymentRefundedEventListener listener;

    @Test
    void refundsOrderWhenEventIsReservation() {
        PaymentRefundedEvent event = new PaymentRefundedEvent(ORDER_ID, OrderType.RESERVATION, 1L,
                BigDecimal.valueOf(10000), NOW);

        listener.on(event);

        verify(reservationService).refundOrder(ORDER_ID);
    }

    @Test
    void ignoresEventWhenOrderTypeIsNotReservation() {
        PaymentRefundedEvent event = new PaymentRefundedEvent("AD-1", OrderType.ADVERTISEMENT, 1L,
                BigDecimal.valueOf(10000), NOW);

        listener.on(event);

        verify(reservationService, never()).refundOrder(anyString());
    }
}
