package com.coderhan.lastmission.reservation.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import com.coderhan.lastmission.event.EventQueryPort;
import com.coderhan.lastmission.event.TicketInfo;
import com.coderhan.lastmission.reservation.domain.CheckinProgress;
import com.coderhan.lastmission.reservation.domain.OrderStatus;
import com.coderhan.lastmission.reservation.domain.QrTicketView;
import com.coderhan.lastmission.reservation.domain.ReservationOrder;
import com.coderhan.lastmission.reservation.domain.ReservationOrderItem;
import com.coderhan.lastmission.reservation.domain.TicketQuantity;
import com.coderhan.lastmission.shared.error.BusinessException;
import com.coderhan.lastmission.shared.error.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {
    private static final long USER_ID = 1L;
    private static final long EVENT_ID = 10L;
    private static final long TICKET_ID = 100L;
    private static final String ORDER_ID = "ORD-20260723-000001";
    private static final OffsetDateTime NOW = OffsetDateTime.parse("2026-07-23T10:00:00Z");

    @Mock ReservationRepository repository;
    @Mock EventQueryPort eventQueryPort;
    @Mock WaitingRoomService waitingRoomService;
    @Spy Clock clock = Clock.fixed(Instant.parse("2026-07-23T10:00:00Z"), ZoneOffset.UTC);

    @InjectMocks ReservationService service;

    private static TicketInfo ticketInfo(int price, int maxPerUser) {
        return new TicketInfo(TICKET_ID, "성인권", price, maxPerUser,
                Instant.parse("2026-07-01T00:00:00Z"), Instant.parse("2026-08-01T00:00:00Z"));
    }

    // ---------- createOrder ----------

    @Test
    void createsOrderWithServerSidePriceAndOneRowPerTicket() {
        List<ReservationService.OrderItemRequest> items =
                List.of(new ReservationService.OrderItemRequest(TICKET_ID, BigDecimal.valueOf(1), 2));
        when(repository.nextOrderId(NOW.toLocalDate())).thenReturn(ORDER_ID);
        when(eventQueryPort.getTicketInfo(TICKET_ID)).thenReturn(Optional.of(ticketInfo(10000, 5)));
        when(repository.countPurchasedQuantity(USER_ID, TICKET_ID)).thenReturn(0L);
        when(eventQueryPort.decreaseTicketStock(TICKET_ID, 2)).thenReturn(true);

        BigDecimal realPrice = BigDecimal.valueOf(10000);
        BigDecimal totalAmount = realPrice.multiply(BigDecimal.valueOf(2));
        ReservationOrder order =
                new ReservationOrder(ORDER_ID, USER_ID, EVENT_ID, OrderStatus.PENDING, totalAmount, NOW, NOW);
        when(repository.createOrder(ORDER_ID, USER_ID, EVENT_ID, totalAmount, NOW)).thenReturn(order);

        ReservationOrderItem item1 = new ReservationOrderItem(1L, ORDER_ID, TICKET_ID, realPrice, "qr-1", null);
        ReservationOrderItem item2 = new ReservationOrderItem(2L, ORDER_ID, TICKET_ID, realPrice, "qr-2", null);
        when(repository.addItem(eq(ORDER_ID), eq(TICKET_ID), eq(realPrice), anyString()))
                .thenReturn(item1, item2);

        ReservationService.OrderDetail result = service.createOrder(USER_ID, EVENT_ID, items);

        // 요청에 실린 unitPrice(1원)가 아니라, 서버가 EventQueryPort에서 조회한 실제 가격(10000원)이 그대로 반영돼야 한다
        assertThat(result.order()).isSameAs(order);
        assertThat(result.items()).containsExactly(item1, item2);
        verify(eventQueryPort).decreaseTicketStock(TICKET_ID, 2);
        verify(repository, times(2)).addItem(eq(ORDER_ID), eq(TICKET_ID), eq(realPrice), anyString());
    }

    @Test
    void createOrderMergesQuantityAcrossMultipleLinesOfSameTicket() {
        List<ReservationService.OrderItemRequest> items = List.of(
                new ReservationService.OrderItemRequest(TICKET_ID, BigDecimal.valueOf(1), 1),
                new ReservationService.OrderItemRequest(TICKET_ID, BigDecimal.valueOf(1), 2));
        when(repository.nextOrderId(NOW.toLocalDate())).thenReturn(ORDER_ID);
        when(eventQueryPort.getTicketInfo(TICKET_ID)).thenReturn(Optional.of(ticketInfo(10000, 10)));
        when(repository.countPurchasedQuantity(USER_ID, TICKET_ID)).thenReturn(0L);
        when(eventQueryPort.decreaseTicketStock(TICKET_ID, 3)).thenReturn(true);

        BigDecimal realPrice = BigDecimal.valueOf(10000);
        BigDecimal totalAmount = realPrice.multiply(BigDecimal.valueOf(3));
        ReservationOrder order =
                new ReservationOrder(ORDER_ID, USER_ID, EVENT_ID, OrderStatus.PENDING, totalAmount, NOW, NOW);
        when(repository.createOrder(ORDER_ID, USER_ID, EVENT_ID, totalAmount, NOW)).thenReturn(order);
        when(repository.addItem(eq(ORDER_ID), eq(TICKET_ID), eq(realPrice), anyString()))
                .thenReturn(
                        new ReservationOrderItem(1L, ORDER_ID, TICKET_ID, realPrice, "qr-1", null),
                        new ReservationOrderItem(2L, ORDER_ID, TICKET_ID, realPrice, "qr-2", null),
                        new ReservationOrderItem(3L, ORDER_ID, TICKET_ID, realPrice, "qr-3", null));

        ReservationService.OrderDetail result = service.createOrder(USER_ID, EVENT_ID, items);

        // 재고 차감은 두 줄을 합친 수량(3)으로 한 번만 호출되지만, row는 여전히 티켓 한 장당 하나씩 3개 생겨야 한다
        assertThat(result.items()).hasSize(3);
        verify(eventQueryPort).decreaseTicketStock(TICKET_ID, 3);
        verify(repository, times(3)).addItem(eq(ORDER_ID), eq(TICKET_ID), eq(realPrice), anyString());
    }

    @Test
    void createOrderRejectsEmptyItems() {
        assertThatThrownBy(() -> service.createOrder(USER_ID, EVENT_ID, List.of()))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(ErrorCode.RESERVATION_INVALID_REQUEST));
    }

    @Test
    void createOrderRejectsUnknownTicket() {
        List<ReservationService.OrderItemRequest> items =
                List.of(new ReservationService.OrderItemRequest(TICKET_ID, BigDecimal.valueOf(1), 1));
        when(repository.nextOrderId(NOW.toLocalDate())).thenReturn(ORDER_ID);
        when(eventQueryPort.getTicketInfo(TICKET_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.createOrder(USER_ID, EVENT_ID, items))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(ErrorCode.TICKET_NOT_FOUND));
    }

    @Test
    void createOrderRejectsWhenSaleNotYetStarted() {
        List<ReservationService.OrderItemRequest> items =
                List.of(new ReservationService.OrderItemRequest(TICKET_ID, BigDecimal.valueOf(1), 1));
        when(repository.nextOrderId(NOW.toLocalDate())).thenReturn(ORDER_ID);
        TicketInfo notYetOnSale = new TicketInfo(TICKET_ID, "성인권", 10000, 5,
                Instant.parse("2026-08-01T00:00:00Z"), Instant.parse("2026-09-01T00:00:00Z"));
        when(eventQueryPort.getTicketInfo(TICKET_ID)).thenReturn(Optional.of(notYetOnSale));

        assertThatThrownBy(() -> service.createOrder(USER_ID, EVENT_ID, items))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(ErrorCode.RESERVATION_INVALID_REQUEST));
        verify(eventQueryPort, never()).decreaseTicketStock(anyLong(), anyInt());
    }

    @Test
    void createOrderRejectsWhenExceedingMaxPurchasePerUser() {
        List<ReservationService.OrderItemRequest> items =
                List.of(new ReservationService.OrderItemRequest(TICKET_ID, BigDecimal.valueOf(1), 3));
        when(repository.nextOrderId(NOW.toLocalDate())).thenReturn(ORDER_ID);
        when(eventQueryPort.getTicketInfo(TICKET_ID)).thenReturn(Optional.of(ticketInfo(10000, 4)));
        when(repository.countPurchasedQuantity(USER_ID, TICKET_ID)).thenReturn(2L);

        assertThatThrownBy(() -> service.createOrder(USER_ID, EVENT_ID, items))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(ErrorCode.RESERVATION_INVALID_REQUEST));
        verify(eventQueryPort, never()).decreaseTicketStock(anyLong(), anyInt());
    }

    @Test
    void createOrderRejectsWhenStockInsufficient() {
        List<ReservationService.OrderItemRequest> items =
                List.of(new ReservationService.OrderItemRequest(TICKET_ID, BigDecimal.valueOf(1), 2));
        when(repository.nextOrderId(NOW.toLocalDate())).thenReturn(ORDER_ID);
        when(eventQueryPort.getTicketInfo(TICKET_ID)).thenReturn(Optional.of(ticketInfo(10000, 5)));
        when(repository.countPurchasedQuantity(USER_ID, TICKET_ID)).thenReturn(0L);
        when(eventQueryPort.decreaseTicketStock(TICKET_ID, 2)).thenReturn(false);

        assertThatThrownBy(() -> service.createOrder(USER_ID, EVENT_ID, items))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(ErrorCode.TICKET_SOLD_OUT));
        verify(repository, never()).createOrder(any(), anyLong(), anyLong(), any(), any());
    }

    // ---------- getOrder ----------

    @Test
    void getOrderReturnsOwnersOrderWithItems() {
        ReservationOrder order = new ReservationOrder(ORDER_ID, USER_ID, EVENT_ID, OrderStatus.CONFIRMED,
                BigDecimal.valueOf(10000), NOW, NOW);
        List<ReservationOrderItem> items = List.of(
                new ReservationOrderItem(1L, ORDER_ID, TICKET_ID, BigDecimal.valueOf(10000), "qr-1", null));
        when(repository.findOrder(ORDER_ID)).thenReturn(Optional.of(order));
        when(repository.findItems(ORDER_ID)).thenReturn(items);

        ReservationService.OrderDetail result = service.getOrder(USER_ID, ORDER_ID);

        assertThat(result.order()).isSameAs(order);
        assertThat(result.items()).isSameAs(items);
    }

    @Test
    void getOrderThrowsWhenNotFound() {
        when(repository.findOrder(ORDER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getOrder(USER_ID, ORDER_ID))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(ErrorCode.RESERVATION_NOT_FOUND));
    }

    @Test
    void getOrderThrowsWhenAccessedByNonOwner() {
        ReservationOrder order = new ReservationOrder(ORDER_ID, USER_ID, EVENT_ID, OrderStatus.CONFIRMED,
                BigDecimal.valueOf(10000), NOW, NOW);
        when(repository.findOrder(ORDER_ID)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.getOrder(999L, ORDER_ID))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(ErrorCode.RESERVATION_ACCESS_DENIED));
    }

    // ---------- getMyOrders ----------

    @Test
    void getMyOrdersFillsMissingQuantitiesWithEmptyList() {
        ReservationOrder orderWithQuantities = new ReservationOrder("ORD-1", USER_ID, EVENT_ID,
                OrderStatus.CONFIRMED, BigDecimal.valueOf(10000), NOW, NOW);
        ReservationOrder orderWithoutQuantities = new ReservationOrder("ORD-2", USER_ID, EVENT_ID,
                OrderStatus.PENDING, BigDecimal.valueOf(5000), NOW, NOW);
        when(repository.findOrdersByUserId(USER_ID, null, 0, 10))
                .thenReturn(new OrderPage(List.of(orderWithQuantities, orderWithoutQuantities), 0, 10, 2));
        when(repository.findTicketQuantitiesByOrderIds(List.of("ORD-1", "ORD-2")))
                .thenReturn(Map.of("ORD-1", List.of(new TicketQuantity(TICKET_ID, 2))));

        ReservationService.MyOrdersPage result = service.getMyOrders(USER_ID, null, 0, 10);

        assertThat(result.orders()).hasSize(2);
        assertThat(result.orders().get(0).ticketQuantities()).containsExactly(new TicketQuantity(TICKET_ID, 2));
        assertThat(result.orders().get(1).ticketQuantities()).isEmpty();
        assertThat(result.totalElements()).isEqualTo(2);
    }

    // ---------- getMyQrTickets ----------

    @Test
    void getMyQrTicketsDelegatesToRepository() {
        List<QrTicketView> tickets = List.of(new QrTicketView(ORDER_ID, EVENT_ID, 1L, TICKET_ID, "qr-1", null));
        when(repository.findQrTicketsByUserId(USER_ID)).thenReturn(tickets);

        assertThat(service.getMyQrTickets(USER_ID)).isSameAs(tickets);
    }

    // ---------- checkin ----------

    @Test
    void checkinMarksItemAsCheckedIn() {
        when(repository.checkin("qr-1", 9L, NOW)).thenReturn(true);
        ReservationOrderItem checkedIn =
                new ReservationOrderItem(1L, ORDER_ID, TICKET_ID, BigDecimal.valueOf(10000), "qr-1", NOW);
        when(repository.findItemByQrCodeHash("qr-1")).thenReturn(Optional.of(checkedIn));

        ReservationOrderItem result = service.checkin(9L, "qr-1");

        assertThat(result).isEqualTo(checkedIn);
    }

    @Test
    void checkinThrowsWhenAlreadyCheckedIn() {
        when(repository.checkin("qr-1", 9L, NOW)).thenReturn(false);
        ReservationOrderItem existing = new ReservationOrderItem(1L, ORDER_ID, TICKET_ID, BigDecimal.valueOf(10000),
                "qr-1", OffsetDateTime.parse("2026-07-23T09:00:00Z"));
        when(repository.findItemByQrCodeHash("qr-1")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> service.checkin(9L, "qr-1"))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(ErrorCode.RESERVATION_ALREADY_CHECKED_IN));
    }

    @Test
    void checkinThrowsWhenQrNotFound() {
        when(repository.checkin("unknown-qr", 9L, NOW)).thenReturn(false);
        when(repository.findItemByQrCodeHash("unknown-qr")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.checkin(9L, "unknown-qr"))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.errorCode()).isEqualTo(ErrorCode.RESERVATION_QR_NOT_FOUND));
    }

    // ---------- 관리자 조회 / ReservationQueryPort ----------

    @Test
    void getEventOrdersDelegatesToRepository() {
        List<ReservationOrder> orders = List.of(
                new ReservationOrder(ORDER_ID, USER_ID, EVENT_ID, OrderStatus.CONFIRMED, BigDecimal.TEN, NOW, NOW));
        when(repository.findOrdersByEventId(EVENT_ID)).thenReturn(orders);

        assertThat(service.getEventOrders(EVENT_ID)).isSameAs(orders);
    }

    @Test
    void getEventSummarySumsCountsAcrossStatuses() {
        Map<OrderStatus, Long> counts = Map.of(
                OrderStatus.PENDING, 3L,
                OrderStatus.CONFIRMED, 7L,
                OrderStatus.CANCELLED, 1L);
        when(repository.countOrdersByEventIdGroupedByStatus(EVENT_ID)).thenReturn(counts);

        ReservationService.EventReservationSummary summary = service.getEventSummary(EVENT_ID);

        assertThat(summary.eventId()).isEqualTo(EVENT_ID);
        assertThat(summary.totalOrders()).isEqualTo(11L);
        assertThat(summary.countsByStatus()).isEqualTo(counts);
    }

    @Test
    void getCheckinProgressDelegatesToRepository() {
        CheckinProgress progress = new CheckinProgress(100, 42);
        when(repository.countCheckinProgressByEventId(EVENT_ID)).thenReturn(progress);

        assertThat(service.getCheckinProgress(EVENT_ID)).isSameAs(progress);
    }

    @Test
    void hasActiveReservationsForEventDelegatesToRepository() {
        when(repository.hasActiveOrdersForEvent(EVENT_ID)).thenReturn(true);

        assertThat(service.hasActiveReservationsForEvent(EVENT_ID)).isTrue();
    }

    @Test
    void hasActiveReservationsForTicketDelegatesToRepository() {
        when(repository.hasActiveOrderItemsForTicket(TICKET_ID)).thenReturn(false);

        assertThat(service.hasActiveReservationsForTicket(TICKET_ID)).isFalse();
    }
}
