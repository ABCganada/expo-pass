package com.coderhan.lastmission.reservation.infrastructure.persistence;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import com.coderhan.lastmission.reservation.application.ReservationRepository;
import com.coderhan.lastmission.reservation.domain.OrderStatus;
import com.coderhan.lastmission.reservation.domain.ReservationOrder;
import com.coderhan.lastmission.reservation.domain.ReservationOrderItem;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * 예약 저장소. reservation_orders / reservation_order_items 만 읽고 쓴다.
 *
 * 애노테이션 없는 순수 record 로 유지하고, Entity ↔ record 변환은 이 클래스에서만 한다.</p>
 */
@Repository
@RequiredArgsConstructor
class JpaReservationRepository implements ReservationRepository {
    private static final DateTimeFormatter ORDER_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final ReservationOrderJpaRepository orderJpaRepository;
    private final ReservationOrderItemJpaRepository itemJpaRepository;
    private final EntityManager entityManager;

    @Override
    public String nextOrderId(LocalDate today) {
        Number sequenceValue = (Number) entityManager
                .createNativeQuery("select nextval('reservation_order_seq')")
                .getSingleResult();
        return "ORD-" + today.format(ORDER_DATE_FORMAT) + "-" + String.format("%06d", sequenceValue.longValue());
    }

    @Override
    public ReservationOrder createOrder(String orderId, long userId, long eventId, BigDecimal totalAmount,
            OffsetDateTime now) {
        ReservationOrderEntity saved = orderJpaRepository.save(
                new ReservationOrderEntity(orderId, userId, eventId, OrderStatus.PENDING, totalAmount, now, now));
        return toDomain(saved);
    }

    @Override
    public ReservationOrderItem addItem(String orderId, long ticketId, BigDecimal unitPrice) {
        ReservationOrderItemEntity saved = itemJpaRepository.save(
                new ReservationOrderItemEntity(orderId, ticketId, unitPrice));
        return toDomain(saved);
    }

    @Override
    public Optional<ReservationOrder> findOrder(String orderId) {
        return orderJpaRepository.findById(orderId).map(JpaReservationRepository::toDomain);
    }

    @Override
    public List<ReservationOrderItem> findItems(String orderId) {
        return itemJpaRepository.findByOrderId(orderId).stream()
                .map(JpaReservationRepository::toDomain)
                .toList();
    }

    private static ReservationOrder toDomain(ReservationOrderEntity entity) {
        return new ReservationOrder(entity.getOrderId(), entity.getUserId(), entity.getEventId(),
                entity.getStatus(), entity.getTotalAmount(), entity.getReservedAt(), entity.getUpdatedAt());
    }

    private static ReservationOrderItem toDomain(ReservationOrderItemEntity entity) {
        return new ReservationOrderItem(entity.getOrderItemId(), entity.getOrderId(), entity.getTicketId(),
                entity.getUnitPrice(), entity.getQrCodeHash());
    }
}
