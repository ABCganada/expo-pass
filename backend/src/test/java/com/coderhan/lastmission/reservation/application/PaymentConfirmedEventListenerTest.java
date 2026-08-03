package com.coderhan.lastmission.reservation.application;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.OffsetDateTime;
import com.coderhan.lastmission.shared.event.PaymentConfirmedEvent;
import com.coderhan.lastmission.shared.order.OrderType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentConfirmedEventListenerTest {
    private static final String ORDER_ID = "ORD-20260723-000001";
    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-07-23T10:00:00Z");

    @Mock ReservationService reservationService;

    @InjectMocks PaymentConfirmedEventListener listener;

    @Test
    void confirmsOrderWhenEventIsReservation() {
        PaymentConfirmedEvent event = new PaymentConfirmedEvent(1L, ORDER_ID, OrderType.RESERVATION, NOW);

        listener.on(event);

        verify(reservationService).confirmOrder(ORDER_ID);
    }

    @Test
    void ignoresEventWhenOrderTypeIsNotReservation() {
        PaymentConfirmedEvent event = new PaymentConfirmedEvent(1L, "AD-1", OrderType.ADVERTISEMENT, NOW);

        listener.on(event);

        verify(reservationService, never()).confirmOrder(anyString());
    }
}
