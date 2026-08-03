package com.coderhan.lastmission.reservation.application;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import com.coderhan.lastmission.shared.order.PaymentOrderDirectory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReservationReconcileSchedulerTest {
    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-07-23T10:00:00Z");
    private static final OffsetDateTime THRESHOLD = NOW.minusMinutes(15);

    @Mock ReservationRepository repository;
    @Mock ReservationService reservationService;
    @Mock PaymentOrderDirectory paymentOrderDirectory;
    @Spy Clock clock = Clock.fixed(Instant.parse("2026-07-23T10:00:00Z"), ZoneOffset.UTC);

    @InjectMocks ReservationReconcileScheduler scheduler;

    @Test
    void doesNothingWhenNoPendingOrdersAreStale() {
        when(repository.findPendingOrderIdsOlderThan(THRESHOLD)).thenReturn(List.of());

        scheduler.reconcile();

        verifyNoInteractions(paymentOrderDirectory);
        verify(reservationService, never()).confirmOrder(anyString());
        verify(reservationService, never()).cancelOrder(anyString());
    }

    @Test
    void confirmsOrderWhenPaymentWasActuallyCompleted() {
        when(repository.findPendingOrderIdsOlderThan(THRESHOLD)).thenReturn(List.of("ORD-1", "ORD-2"));
        when(paymentOrderDirectory.findCompletedOrderIds(List.of("ORD-1", "ORD-2"))).thenReturn(List.of("ORD-1"));

        scheduler.reconcile();

        verify(reservationService).confirmOrder("ORD-1");
        verify(reservationService, never()).cancelOrder("ORD-1");
    }

    @Test
    void cancelsOrderWhenPaymentWasNeverCompleted() {
        when(repository.findPendingOrderIdsOlderThan(THRESHOLD)).thenReturn(List.of("ORD-1", "ORD-2"));
        when(paymentOrderDirectory.findCompletedOrderIds(List.of("ORD-1", "ORD-2"))).thenReturn(List.of("ORD-1"));

        scheduler.reconcile();

        verify(reservationService).cancelOrder("ORD-2");
        verify(reservationService, never()).confirmOrder("ORD-2");
    }
}
