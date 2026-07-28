package com.coderhan.lastmission.reservation.application;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.IntStream;
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
    private final Clock clock;

    /**
     * 주문을 생성한다.
     */
    @Transactional
    public OrderDetail createOrder(long userId, long eventId, List<OrderItemRequest> items) {
        ReservationOrder.validateEventId(eventId);
        if (items == null || items.isEmpty()) {
            throw new BusinessException(ErrorCode.RESERVATION_INVALID_REQUEST, "주문할 티켓 항목이 없습니다.");
        }
        items.forEach(item -> ReservationOrderItem.validate(item.ticketId(), item.unitPrice(), item.quantity()));

        OffsetDateTime now = OffsetDateTime.now(clock);
        String orderId = repository.nextOrderId(now.toLocalDate());
        BigDecimal totalAmount = items.stream()
                .map(item -> item.unitPrice().multiply(BigDecimal.valueOf(item.quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        ReservationOrder order = repository.createOrder(orderId, userId, eventId, totalAmount, now);
        // 티켓 한 장 = row 한 개. 같은 티켓을 quantity장 사면 addItem을 quantity번 호출해 각 장을 개별 row로 만든다
        // (장마다 현장에서 독립적으로 QR 체크인되어야 하므로 하나의 row에 quantity로 뭉쳐두지 않는다).
        List<ReservationOrderItem> savedItems = items.stream()
                .flatMap(item -> IntStream.range(0, item.quantity())
                        .mapToObj(ignored -> repository.addItem(orderId, item.ticketId(), item.unitPrice())))
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

    public record OrderItemRequest(long ticketId, BigDecimal unitPrice, int quantity) {}

    public record OrderDetail(ReservationOrder order, List<ReservationOrderItem> items) {}
}
