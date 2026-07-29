package com.coderhan.lastmission.reservation.application;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import com.coderhan.lastmission.event.EventQueryPort;
import com.coderhan.lastmission.event.TicketInfo;
import com.coderhan.lastmission.reservation.domain.OrderStatus;
import com.coderhan.lastmission.reservation.domain.ReservationOrder;
import com.coderhan.lastmission.reservation.domain.ReservationOrderItem;
import com.coderhan.lastmission.shared.error.BusinessException;
import com.coderhan.lastmission.shared.error.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReservationService {
    private final ReservationRepository repository;
    private final EventQueryPort eventQueryPort;
    private final WaitingRoomService waitingRoomService;   // ← 추가
    private final Clock clock;

    /**
     * 주문을 생성한다.
     */
    @Transactional
    public OrderDetail createOrder(long userId, long eventId, List<OrderItemRequest> items) {
        waitingRoomService.consumeAdmission(userId, eventId);
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

        ReservationOrder order = repository.createOrder(orderId, userId, eventId, totalAmount, now);
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
     * 이 유저의 모든 주문을 최신순으로 조회한다(목록용, 아이템은 안 채움).
     */
    @Transactional(readOnly = true)
    public List<ReservationOrder> getMyOrders(long userId) {
        return repository.findOrdersByUserId(userId);
    }

    /**
     * 관리자 QR 체크인. 이미 체크인됐거나 존재하지 않는 QR이면 예외.
     */
    @Transactional
    public ReservationOrderItem checkin(long adminUserId, String qrCodeHash) {
        OffsetDateTime now = OffsetDateTime.now(clock);
        boolean checkedIn = repository.checkin(qrCodeHash, adminUserId, now);
        if (!checkedIn) {
            ReservationOrderItem existing = repository.findItemByQrCodeHash(qrCodeHash)
                    .orElseThrow(() -> new BusinessException(ErrorCode.RESERVATION_QR_NOT_FOUND,
                            "존재하지 않는 QR입니다."));
            throw new BusinessException(ErrorCode.RESERVATION_ALREADY_CHECKED_IN,
                    "이미 체크인된 티켓입니다. (체크인 시각: " + existing.checkedInAt() + ")");
        }
        return repository.findItemByQrCodeHash(qrCodeHash).orElseThrow();
    }

    /**
     * 관리자용 — 이 행사의 모든 주문(예약자 명단)을 최신순으로 조회한다.
     */
    @Transactional(readOnly = true)
    public List<ReservationOrder> getEventOrders(long eventId) {
        return repository.findOrdersByEventId(eventId);
    }

    /**
     * 관리자용 — 이 행사의 예약 현황(상태별 건수)을 조회한다.
     */
    @Transactional(readOnly = true)
    public EventReservationSummary getEventSummary(long eventId) {
        Map<OrderStatus, Long> countsByStatus = repository.countOrdersByEventIdGroupedByStatus(eventId);
        long totalOrders = countsByStatus.values().stream().mapToLong(Long::longValue).sum();
        return new EventReservationSummary(eventId, totalOrders, countsByStatus);
    }

    public record OrderItemRequest(long ticketId, BigDecimal unitPrice, int quantity) {}

    public record OrderDetail(ReservationOrder order, List<ReservationOrderItem> items) {}

    public record EventReservationSummary(
            long eventId, long totalOrders, Map<OrderStatus, Long> countsByStatus) {}
}
