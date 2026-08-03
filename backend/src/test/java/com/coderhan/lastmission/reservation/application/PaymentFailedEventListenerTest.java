package com.coderhan.lastmission.reservation.application;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import java.time.OffsetDateTime;
import com.coderhan.lastmission.shared.event.PaymentFailedEvent;
import com.coderhan.lastmission.shared.order.OrderType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PaymentFailedEventListenerTest {
    private static final String ORDER_ID = "ORD-20260723-000001";
    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-07-23T10:00:00Z");

    @Mock ReservationService reservationService;

    @InjectMocks PaymentFailedEventListener listener;

    @Test
    void cancelsOrderWhenEventIsReservation() {
        PaymentFailedEvent event = new PaymentFailedEvent(ORDER_ID, OrderType.RESERVATION, 1L, "카드 한도 초과", NOW);

        listener.on(event);

        verify(reservationService).cancelOrder(ORDER_ID);
    }

    @Test
    void ignoresEventWhenOrderTypeIsNotReservation() {
        PaymentFailedEvent event = new PaymentFailedEvent("AD-1", OrderType.ADVERTISEMENT, 1L, "카드 한도 초과", NOW);

        listener.on(event);

        verify(reservationService, never()).cancelOrder(anyString());
    }
}
