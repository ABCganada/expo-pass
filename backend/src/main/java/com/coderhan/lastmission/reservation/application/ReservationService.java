package com.coderhan.lastmission.reservation.application;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.coderhan.lastmission.event.EventManagerQueryPort;
import com.coderhan.lastmission.event.EventQueryPort;
import com.coderhan.lastmission.event.ReservationQueryPort;
import com.coderhan.lastmission.event.TicketInfo;
import com.coderhan.lastmission.reservation.domain.*;
import com.coderhan.lastmission.shared.error.BusinessException;
import com.coderhan.lastmission.shared.error.ErrorCode;
import com.coderhan.lastmission.user.UserDirectory;
import com.coderhan.lastmission.user.UserRef;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReservationService implements ReservationQueryPort {
    private final ReservationRepository repository;
    private final EventQueryPort eventQueryPort;
    private final EventManagerQueryPort eventManagerQueryPort;
    private final WaitingRoomService waitingRoomService;
    private final UserDirectory userDirectory;
    private final Clock clock;

    /**
     * 주문을 생성한다.
     */
    @Transactional
    public OrderDetail createOrder(long userId, long eventId, List<OrderItemRequest> items) {
        waitingRoomService.consumeTicket(userId, eventId);
        ReservationOrder.validateEventId(eventId);
        if (items == null || items.isEmpty()) {
            throw new BusinessException(ErrorCode.RESERVATION_INVALID_REQUEST, "주문할 티켓 항목이 없습니다.");
        }
        items.forEach(item -> ReservationOrderItem.validate(item.ticketId(), item.unitPrice(), item.quantity()));

        OffsetDateTime now = OffsetDateTime.now(clock);
        String orderId = repository.nextOrderId(now.toLocalDate());

        Map<Long, TicketInfo> ticketInfoByTicketId = items.stream()
                .map(ReservationService.OrderItemRequest::ticketId)
                .distinct()
                .collect(Collectors.toMap(id -> id, id -> eventQueryPort.getTicketInfo(id)
                        .orElseThrow(() -> new BusinessException(ErrorCode.TICKET_NOT_FOUND,
                                "존재하지 않는 티켓입니다. ticketId=" + id))));

        // 이 주문에 등장하는 티켓 종류별로 필요한 수량을 합산 (같은 티켓이 여러 줄로 나뉘어 왔을 경우 대비)
        Map<Long, Integer> quantityByTicketId = items.stream()
                .collect(Collectors.groupingBy(OrderItemRequest::ticketId, Collectors.summingInt(OrderItemRequest::quantity)));

        quantityByTicketId.forEach((ticketId, quantity) -> {
            TicketInfo ticketInfo = ticketInfoByTicketId.get(ticketId);

            Instant nowInstant = now.toInstant();
            if (nowInstant.isBefore(ticketInfo.saleStartAt()) || nowInstant.isAfter(ticketInfo.saleEndAt())) {
                throw new BusinessException(ErrorCode.RESERVATION_INVALID_REQUEST, "지금은 판매 기간이 아닙니다.");
            }

            long alreadyPurchased = repository.countPurchasedQuantity(userId, ticketId);
            if (alreadyPurchased + quantity > ticketInfo.maxPurchasePerUser()) {
                throw new BusinessException(ErrorCode.RESERVATION_INVALID_REQUEST,
                        "1인당 구매 가능 수량(" + ticketInfo.maxPurchasePerUser() + "장)을 초과했습니다.");
            }

            boolean decreased = eventQueryPort.decreaseTicketStock(ticketId, quantity);
            if (!decreased) {
                throw new BusinessException(ErrorCode.TICKET_SOLD_OUT, "재고가 부족합니다. ticketId=" + ticketId);
            }
        });

        BigDecimal totalAmount = items.stream()
                .map(item -> {
                    BigDecimal realPrice = BigDecimal.valueOf(ticketInfoByTicketId.get(item.ticketId()).price());
                    return realPrice.multiply(BigDecimal.valueOf(item.quantity()));
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // 무료(0원) 주문은 결제 자체가 필요 없으므로 PENDING을 거치지 않고 바로 CONFIRMED로 생성한다.
        OrderStatus initialStatus = totalAmount.signum() == 0 ? OrderStatus.CONFIRMED : OrderStatus.PENDING;
        ReservationOrder order = repository.createOrder(orderId, userId, eventId, totalAmount, initialStatus, now);
        // 티켓 한 장 = row 한 개. 같은 티켓을 quantity장 사면 addItem을 quantity번 호출해 각 장을 개별 row로 만든다
        // (장마다 현장에서 독립적으로 QR 체크인되어야 하므로 하나의 row에 quantity로 뭉쳐두지 않는다).
        List<ReservationOrderItem> savedItems = items.stream()
                .flatMap(item -> {
                    BigDecimal realPrice = BigDecimal.valueOf(ticketInfoByTicketId.get(item.ticketId()).price());
                    return IntStream.range(0, item.quantity())
                            .mapToObj(ignored -> repository.addItem(orderId, item.ticketId(), realPrice, UUID.randomUUID().toString()));
                })
                .toList();
        return new OrderDetail(order, savedItems);
    }

    @Transactional(readOnly = true)
    public OrderDetail getOrder(long currentUserId, String orderId) {
        ReservationOrder order = repository.findOrder(orderId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_NOT_FOUND, "주문을 찾을 수 없습니다."));
        if (order.userId() != currentUserId) {
            throw new BusinessException(ErrorCode.RESERVATION_ACCESS_DENIED, "본인의 주문만 조회할 수 있습니다.");
        }
        return new OrderDetail(order, repository.findItems(orderId));
    }

    /**
     * 이 유저의 주문을 최신순으로 페이지 단위로 조회한다(목록용). 아이템 자체는 안 채우고,
     * 이 페이지에 나온 주문에 대해서만 티켓 종류별 수량을 일괄 집계해서 같이 내려준다
     * (주문마다 상세를 또 조회하는 N+1 방지 — 전체가 아니라 이 페이지 분량만 조회한다).
     * status가 null이면 전체 상태를 대상으로 한다(마이페이지 필터 탭).
     */
    @Transactional(readOnly = true)
    public MyOrdersPage getMyOrders(long userId, OrderStatus status, int page, int size) {
        OrderPage orderPage = repository.findOrdersByUserId(userId, status, page, size);
        List<String> orderIds = orderPage.orders().stream().map(ReservationOrder::orderId).toList();
        Map<String, List<TicketQuantity>> quantitiesByOrderId = repository.findTicketQuantitiesByOrderIds(orderIds);
        List<OrderWithTickets> orders = orderPage.orders().stream()
                .map(order -> new OrderWithTickets(order, quantitiesByOrderId.getOrDefault(order.orderId(), List.of())))
                .toList();
        return new MyOrdersPage(orders, orderPage.page(), orderPage.size(), orderPage.totalElements());
    }

    /**
     * 이 유저의 QR 발급 대상 티켓(취소/환불 제외)을 전부 조회한다(QR 티켓 화면용).
     * 주문마다 상세를 따로 조회하지 않고 한 번의 쿼리로 다 가져온다.
     */
    @Transactional(readOnly = true)
    public List<QrTicketView> getMyQrTickets(long userId) {
        return repository.findQrTicketsByUserId(userId);
    }

    /**
     * 관리자 QR 체크인. 본인이 담당하는 행사가 아니면 거부 — 그래서 원래는 조건부 UPDATE 하나로
     * 끝나던 걸, eventId를 먼저 알아야 권한을 검증할 수 있어 주문 조회가 선행된다.
     * 존재하지 않는 QR·이미 체크인됨·주문이 CONFIRMED가 아님(결제대기/취소/환불)이면 예외 —
     * 특히 환불된 결제의 QR을 캡처해뒀다가 현장에서 스캔하는 경우를 막기 위해,
     * DB 쪽 조건부 UPDATE 자체가 주문 상태까지 함께 확인한다(체크인 성공은 CONFIRMED일 때만).
     */
    @Transactional
    public ReservationOrderItem checkin(long callerUserId, String qrCodeHash) {
        ReservationOrderItem existing = repository.findItemByQrCodeHash(qrCodeHash)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_QR_NOT_FOUND,
                        "존재하지 않는 QR입니다."));
        ReservationOrder order = repository.findOrder(existing.orderId())
                .orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_NOT_FOUND, "주문을 찾을 수 없습니다."));
        validateManagerAccess(order.eventId(), callerUserId);

        OffsetDateTime now = OffsetDateTime.now(clock);
        boolean checkedIn = repository.checkin(qrCodeHash, callerUserId, now);
        if (!checkedIn) {
            if (existing.checkedInAt() != null) {
                throw new BusinessException(ErrorCode.RESERVATION_ALREADY_CHECKED_IN,
                        "이미 체크인된 티켓입니다. (체크인 시각: " + existing.checkedInAt() + ")");
            }
            throw new BusinessException(ErrorCode.RESERVATION_INVALID_TICKET_STATUS,
                    "체크인할 수 없는 티켓입니다. (주문 상태: " + order.status() + ")");
        }
        return repository.findItemByQrCodeHash(qrCodeHash).orElseThrow();
    }

    /**
     * 결제 승인(PaymentConfirmedEvent) 시 호출 — PENDING 주문을 CONFIRMED로 전환한다.
     * 이미 다른 상태로 바뀌었거나 존재하지 않는 주문이면 조용히 무시한다(이벤트는 재전달·중복
     * 처리될 수 있으므로 리스너가 예외 없이 멱등하게 동작해야 한다).
     */
    @Transactional
    public void confirmOrder(String orderId) {
        repository.confirmOrderIfPending(orderId, OffsetDateTime.now(clock));
    }

    /**
     * 결제 환불(PaymentRefundedEvent) 시 호출 — CONFIRMED 주문을 REFUNDED로 전환하고,
     * 주문 시점에 차감했던 티켓 재고를 되돌린다. 이미 다른 상태로 바뀌었거나 존재하지 않는
     * 주문이면 조용히 무시한다(이벤트 재전달에 안전해야 할 뿐 아니라, 재고를 두 번 복원하는
     * 사고를 막기 위해서도 이 가드가 꼭 필요하다 — 그래서 재고 복원 전에 먼저 상태 전환이
     * 실제로 일어났는지부터 확인한다).
     */
    @Transactional
    public void refundOrder(String orderId) {
        boolean refunded = repository.refundOrderIfConfirmed(orderId, OffsetDateTime.now(clock));
        if (!refunded) {
            return;
        }
        Map<Long, Long> quantityByTicketId = repository.findItems(orderId).stream()
                .collect(Collectors.groupingBy(ReservationOrderItem::ticketId, Collectors.counting()));
        quantityByTicketId.forEach((ticketId, quantity) -> eventQueryPort.increaseTicketStock(ticketId, quantity.intValue()));
    }

    /**
     * 결제 실패(PaymentFailedEvent) 시 호출 — PENDING 주문을 CANCELLED로 전환하고, 주문 시점에
     * 차감했던 티켓 재고를 되돌린다. 결제 시도 자체가 실패한 게 확정된 상황이라 보정 스케쥴러의
     * 유휴시간(10~15분)을 기다릴 이유가 없어 즉시 처리한다. refundOrder와 동일하게, 이미 다른
     * 상태로 바뀌었거나 존재하지 않는 주문이면 조용히 무시한다(이벤트 재전달 안전성 + 재고 이중 복원 방지).
     */
    @Transactional
    public void cancelOrder(String orderId) {
        boolean cancelled = repository.cancelOrderIfPending(orderId, OffsetDateTime.now(clock));
        if (!cancelled) {
            return;
        }
        Map<Long, Long> quantityByTicketId = repository.findItems(orderId).stream()
                .collect(Collectors.groupingBy(ReservationOrderItem::ticketId, Collectors.counting()));
        quantityByTicketId.forEach((ticketId, quantity) -> eventQueryPort.increaseTicketStock(ticketId, quantity.intValue()));
    }

    /**
     * 관리자용 — 이 행사의 모든 주문(예약자 명단)을 최신순으로 조회한다.
     */
    @Transactional(readOnly = true)
    public List<ReservationOrder> getEventOrders(long eventId) {
        return repository.findOrdersByEventId(eventId);
    }

    /**
     * 매니저용 — 이 행사의 예약자 명단을 유저 이름/이메일까지 채워서 조회한다.
     * 본인이 담당하는 행사가 아니면 ADMIN이어도 거부(PII가 섞여 있어 소유권 검증이 특히 중요하다).
     */
    @Transactional(readOnly = true)
    public List<AttendeeInfo> getEventAttendees(long eventId, long callerUserId) {
        validateManagerAccess(eventId, callerUserId);
        List<ReservationOrder> orders = repository.findOrdersByEventId(eventId);

        List<Long> userIds = orders.stream().map(ReservationOrder::userId).distinct().toList();
        Map<Long, UserRef> usersById = userDirectory.findActiveByIds(userIds).stream()
                .collect(Collectors.toMap(UserRef::id, ref -> ref));

        return orders.stream()
                .map(order -> new AttendeeInfo(order, usersById.get(order.userId())))
                .toList();
    }

    /**
     * 매니저용 — 이 행사의 예약 현황(상태별 건수)을 조회한다. 본인이 담당하는 행사가 아니면 거부.
     */
    @Transactional(readOnly = true)
    public EventReservationSummary getEventSummary(long eventId, long callerUserId) {
        validateManagerAccess(eventId, callerUserId);
        return buildEventSummary(eventId);
    }

    /**
     * 관리자용 — 소유권과 무관하게 전체 행사를 대상으로 예약 현황을 조회한다.
     */
    @Transactional(readOnly = true)
    public EventReservationSummary getEventSummaryForAdmin(long eventId) {
        return buildEventSummary(eventId);
    }

    private EventReservationSummary buildEventSummary(long eventId) {
        Map<OrderStatus, Long> countsByStatus = repository.countOrdersByEventIdGroupedByStatus(eventId);
        long totalOrders = countsByStatus.values().stream().mapToLong(Long::longValue).sum();
        return new EventReservationSummary(eventId, totalOrders, countsByStatus);
    }

    public CheckinProgress getCheckinProgress(long eventId, long callerUserId) {
        validateManagerAccess(eventId, callerUserId);
        return repository.countCheckinProgressByEventId(eventId);
    }

    /**
     * 매니저용 — 이 행사의 예약 건수를 날짜별로 집계한다(예약 추이 그래프용). 본인이 담당하는 행사가 아니면 거부.
     */
    @Transactional(readOnly = true)
    public List<DailyReservationCount> getDailyReservationCounts(long eventId, long callerUserId) {
        validateManagerAccess(eventId, callerUserId);
        return buildDailyReservationCounts(eventId);
    }

    /**
     * 관리자용 — 소유권과 무관하게 전체 행사를 대상으로 날짜별 예약 건수를 조회한다.
     */
    @Transactional(readOnly = true)
    public List<DailyReservationCount> getDailyReservationCountsForAdmin(long eventId) {
        return buildDailyReservationCounts(eventId);
    }

    // 이벤트당 예약 건수가 크지 않아, 별도 집계 쿼리 없이 기존 목록 조회 결과를 날짜별로 묶는다.
    // 예약 추이는 실제 확정된 예약만 의미가 있으므로 PENDING/CANCELLED/REFUNDED는 제외한다.
    private List<DailyReservationCount> buildDailyReservationCounts(long eventId) {
        Map<LocalDate, Long> countsByDate = repository.findOrdersByEventId(eventId).stream()
                .filter(order -> order.status() == OrderStatus.CONFIRMED)
                .collect(Collectors.groupingBy(order -> order.reservedAt().toLocalDate(), Collectors.counting()));
        return countsByDate.entrySet().stream()
                .map(entry -> new DailyReservationCount(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(DailyReservationCount::date))
                .toList();
    }

    /** 본인이 담당하는 행사인지 확인한다 — ADMIN도 예외 없이 소유권을 검증한다(event 도메인의 EventOwnershipValidator와 동일한 규칙). */
    private void validateManagerAccess(long eventId, long callerUserId) {
        Long managerId = eventManagerQueryPort.findEventManagerId(eventId)
                .orElseThrow(() -> new BusinessException(ErrorCode.EVENT_NOT_FOUND, "행사를 찾을 수 없습니다."));
        if (!Objects.equals(managerId, callerUserId)) {
            throw new BusinessException(ErrorCode.RESERVATION_ACCESS_DENIED, "본인이 담당하는 행사만 관리할 수 있습니다.");
        }
    }

    @Override
    public boolean hasActiveReservationsForEvent(long eventId) {
        return repository.hasActiveOrdersForEvent(eventId);
    }

    @Override
    public boolean hasActiveReservationsForTicket(long ticketId) {
        return repository.hasActiveOrderItemsForTicket(ticketId);
    }

    public record OrderItemRequest(long ticketId, BigDecimal unitPrice, int quantity) {}

    public record OrderDetail(ReservationOrder order, List<ReservationOrderItem> items) {}

    public record OrderWithTickets(ReservationOrder order, List<TicketQuantity> ticketQuantities) {}

    public record MyOrdersPage(List<OrderWithTickets> orders, int page, int size, long totalElements) {}

    public record EventReservationSummary(
            long eventId, long totalOrders, Map<OrderStatus, Long> countsByStatus) {}

    public record DailyReservationCount(LocalDate date, long count) {}

    /** userRef가 null이면(탈퇴 등으로 활성 유저가 아니면) 이름/이메일을 모른다는 뜻이다. */
    public record AttendeeInfo(ReservationOrder order, UserRef userRef) {}
}
